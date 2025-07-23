package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.config.Settings
import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import de.vanfanel.joustmania.hardware.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.hardware.RUMBLE_HARDEST
import de.vanfanel.joustmania.hardware.RUMBLE_MEDIUM
import de.vanfanel.joustmania.hardware.RUMBLE_SOFTEST
import de.vanfanel.joustmania.hardware.psmove.addRumbleEvent
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundId.Companion.colorToSound
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.RainbowAnimation
import de.vanfanel.joustmania.types.Sensibility
import de.vanfanel.joustmania.util.CustomThreadDispatcher
import de.vanfanel.joustmania.util.onlyRemovedFromPrevious
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class FreeForAll : Game {
    private val logger = KotlinLogging.logger {}

    override val name = "FreeForAll"
    override val currentPlayingController: MutableMap<MacAddress, PSMoveStub> = ConcurrentHashMap()
    override val minimumPlayers: Int = 2
    override val gameSelectedSound: SoundId = SoundId.GAME_MODE_FFA

    private val _playerLostFlow: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    override val playerLostFlow: Flow<List<String>> = _playerLostFlow

    private val gameJobs: MutableSet<Job> = mutableSetOf()
    private var disconnectedControllerJob: Job? = null
    private var gameRunning: Boolean = false
    private val playersLost: MutableSet<MacAddress> = mutableSetOf()
    private val playerColors: MutableMap<MacAddress, MoveColor> = mutableMapOf()

    private var currentSensitivity: Sensibility = Sensibility.MEDIUM

    companion object {
        val listOfPlayerColors = listOf(
            MoveColor.GREEN,
        )

        val defaultPlayerColor = MoveColor.WHITE
    }

    private fun getMoveColor(address: MacAddress): MoveColor = playerColors[address] ?: defaultPlayerColor

    private fun initDisconnectionObserver() {
        disconnectedControllerJob = CoroutineScope(Dispatchers.IO).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.onlyRemovedFromPrevious().collect { moves ->
                moves.forEach { move ->
                    logger.info { "Move with address: ${move.macAddress} was disconnected. Set Player to game Over" }
                    playersLost.add(move.macAddress)
                    _playerLostFlow.emit(playersLost.toList())
                }
            }
        }
    }

    private fun initObservers(stubs: MutableSet<MacAddress>) {
        stubs.forEach { stub ->
            gameJobs.add(observeAcceleration(stub))
        }
    }

    override fun cleanUpGame() {
        gameJobs.forEach { it.cancel("FreeForAll game go cleanup call") }
        disconnectedControllerJob?.cancel("FreeForAll game go cleanup call")
    }

    private fun observeAcceleration(stubId: MacAddress): Job {
        val currentJob = CoroutineScope(CustomThreadDispatcher.GAME_LOOP).launch {
            val stub = currentPlayingController[stubId]
            if (stub == null) {
                return@launch
            }

            stub.accelerationFlow.collect { acceleration ->
                if (acceleration.change > 1.2 && gameRunning && !playersLost.contains(stub.macAddress)) {
                    if (acceleration.change > currentSensitivity.getSensibilityValues().deathThreshold) {
                        logger.info { "FFA: Move ${stub.macAddress} has acceleration ${acceleration.change} and lost the game" }
                        val randomSoundId = listOf(SoundId.PLAYER_LOSE_1, SoundId.PLAYER_LOSE_2).random()
                        SoundManager.asyncAddSoundToQueue(id = randomSoundId, abortOnNewSound = false)
                        stub.setColorAnimation(
                            ColorAnimation(
                                colorToSet = listOf(
                                    MoveColor.RED,
                                    MoveColor.RED_INACTIVE,
                                    MoveColor.RED,
                                    MoveColor.RED_INACTIVE,
                                    MoveColor.BLACK
                                ), durationInMS = 3000, loop = false
                            )
                        )
                        addRumbleEvent(move = stub.macAddress, intensity = RUMBLE_HARDEST, durationInMs = 3000)
                        playersLost.add(stub.macAddress)
                        _playerLostFlow.emit(playersLost.toList())
                        delay(3100) // add some delay to get sure animation was stopped
                        stub.setCurrentColor(colorToSet = MoveColor.BLACK)
                    } else if (acceleration.change > currentSensitivity.getSensibilityValues().warningThreshold) {
                        logger.info { "FFA: Move ${stub.macAddress} has acceleration ${acceleration.change} and got a warning" }
                        addRumbleEvent(move = stub.macAddress, intensity = RUMBLE_MEDIUM, durationInMs = 1000)
                        stub.setColorAnimation(
                            ColorAnimation(
                                colorToSet = listOf(
                                    MoveColor.ORANGE, getMoveColor(stub.macAddress)
                                ), durationInMS = 1000, loop = false
                            )
                        )
                    }
                }
            }
        }
        return currentJob
    }

    override suspend fun checkForGameFinished() {
        val allPlayers = currentPlayingController.keys

        if (allPlayers.size - playersLost.size == 1) {
            gameRunning = false
            val winner = (allPlayers - playersLost).first()
            playWinnerAnimationAndChangeGameState(winner)
            return
        }

        // should not happen, only if inside 5 ms every remaining player was defeated
        if (allPlayers == playersLost) {
            gameRunning = false
            val winner = playersLost.last()
            playWinnerAnimationAndChangeGameState(winner)
            return
        }
    }

    override suspend fun forceGameEnd() {
        gameRunning = false
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()
        //TODO Play game forced to stop sound
        GameStateManager.setGameFinished()
    }

    private suspend fun playWinnerAnimationAndChangeGameState(winner: MacAddress) {
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()

        val winnerStub: PSMoveStub? = currentPlayingController[winner]
        winnerStub?.setColorAnimation(animation = RainbowAnimation)

        val colorOfWinner = playerColors[winner]
        val colorWinsSound = colorToSound(colorOfWinner)

        colorWinsSound?.let {
            SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
                id = it,
                abortOnNewSound = false,
                minDelay = 16000L
            )
        }
        winnerStub?.clearAnimation()
        PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)
        delay(1000)
        GameStateManager.setGameFinished()
    }

    override suspend fun start(players: Set<PSMoveStub>) {
        initDisconnectionObserver()
        delay(100) // give the Lobby some time to kill all jobs
        // set game sensitivity
        currentSensitivity = Settings.getSensibility()
        logger.info { "Set current Sensitivity to ${currentSensitivity.getSensibilityValues()}" }

        PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)

        currentPlayingController.clear()
        currentPlayingController.putAll(players.associateBy { it.macAddress })
        initObservers(currentPlayingController.keys)

        // set player colors
        currentPlayingController.onEachIndexed { index, player ->
            val color = listOfPlayerColors[index % listOfPlayerColors.size]
            player.value.setCurrentColor(color)
            playerColors[player.key] = color
        }

        currentPlayingController.forEach { player ->
            player.value.setColorAnimation(RainbowAnimation)
        }



        SoundManager.clearSoundQueue()
        logger.info { "play explanation..." }
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.GAME_MODE_FFA_EXPLANATION,
            abortOnNewSound = false
        )
        logger.info { "explanation played" }


        currentPlayingController.forEach { player -> player.value.setCurrentColor(MoveColor.RED) }
        currentPlayingController.forEach { player ->
            player.value.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.RED, MoveColor.RED_INACTIVE
                    ), durationInMS = 1000, loop = false
                )
            )
        }
        PSMoveApi.rumble(
            moves = currentPlayingController.keys,
            intensity = RUMBLE_SOFTEST,
            durationInMs = 500
        )


        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.THREE,
            abortOnNewSound = false,
            minDelay = 1000L
        )
        currentPlayingController.forEach { player ->
            player.value.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.YELLOW, MoveColor.YELLOW_INACTIVE
                    ), durationInMS = 1000, loop = false
                )
            )
        }
        PSMoveApi.rumble(
            moves = currentPlayingController.keys,
            intensity = RUMBLE_MEDIUM,
            durationInMs = 500
        )

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.TWO,
            abortOnNewSound = false,
            minDelay = 1000L
        )


        currentPlayingController.forEach { player ->
            player.value.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.GREEN, MoveColor.GREEN_INACTIVE
                    ), durationInMS = 1000, loop = false
                )
            )
        }
        PSMoveApi.rumble(
            moves = currentPlayingController.keys,
            intensity = RUMBLE_HARDEST,
            durationInMs = 500
        )


        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.ONE,
            abortOnNewSound = false,
            minDelay = 1000L
        )

        currentPlayingController.forEach { player ->
            player.value.setCurrentColor(colorToSet = getMoveColor(player.key))
        }


        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(id = SoundId.GO, abortOnNewSound = false)

        this.gameRunning = true
        GameStateManager.setGameRunning()
    }
}