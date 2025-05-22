package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.hardware.psmove.RUMBLE_HARDEST
import de.vanfanel.joustmania.hardware.psmove.RUMBLE_MEDIUM
import de.vanfanel.joustmania.hardware.psmove.RUMBLE_SOFT
import de.vanfanel.joustmania.hardware.psmove.RUMBLE_SOFTEST
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundId.Companion.colorToSound
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.RainbowAnimation
import de.vanfanel.joustmania.util.onlyRemovedFromPrevious
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FreeForAll : Game {
    private val logger = KotlinLogging.logger {}

    override val name = "FreeForAll"
    override val currentPlayingController: MutableSet<PSMoveStub> = mutableSetOf()
    private val gameJobs: MutableSet<Job> = mutableSetOf()
    private var disconnectedControllerJob: Job? = null
    private var gameLoopJob: Job? = null
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

    private fun getMoveColor(stub: PSMoveStub): MoveColor = playerColors[stub.macAddress] ?: defaultPlayerColor

    private fun initDisconnectionObserver() {
        disconnectedControllerJob = CoroutineScope(Dispatchers.IO).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.onlyRemovedFromPrevious().collect { moves ->
                moves.forEach { move ->
                    logger.info { "Move with address: ${move.macAddress} was disconnected. Set Player to game Over" }
                    playersLost.add(move.macAddress)
                }
            }
        }
    }

    private fun initObservers(currentPlayingController: MutableSet<PSMoveStub>) {
        currentPlayingController.forEach { stub ->
            gameJobs.add(observeAcceleration(stub))
        }
    }

    override fun cleanUpGame() {
        gameJobs.forEach { it.cancel("FreeForAll game go cleanup call") }
        gameLoopJob?.cancel("FreeForAll game go cleanup call")
        disconnectedControllerJob?.cancel("FreeForAll game go cleanup call")
    }

    private fun observeAcceleration(stub: PSMoveStub): Job {
        val currentJob = CoroutineScope(Dispatchers.IO).launch {
            stub.accelerationFlow.collect { acceleration ->
                if (acceleration.change > 1.2 && gameRunning && !playersLost.contains(stub.macAddress)) {
                    if (acceleration.change > currentSensitivity.getSensibilityValues().deathThreshold) {
                        logger.info { "FFA: Move ${stub.macAddress} has acceleration ${acceleration.change} and lost the game" }
                        PSMoveApi.stopRumble(macAddress = stub.macAddress)
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
                        PSMoveApi.rumble(macAddress = stub.macAddress, intensity = RUMBLE_HARDEST, 3000)
                        playersLost.add(stub.macAddress)
                        delay(3100) // add some delay to get sure animation was stopped
                        stub.setCurrentColor(colorToSet = MoveColor.BLACK)
                    } else if (acceleration.change > currentSensitivity.getSensibilityValues().warningThreshold) {
                        logger.info { "FFA: Move ${stub.macAddress} has acceleration ${acceleration.change} and got a warning" }
                        PSMoveApi.stopRumble(macAddress = stub.macAddress)
                        PSMoveApi.rumble(macAddress = stub.macAddress, intensity = RUMBLE_MEDIUM, 1000)
                        stub.setColorAnimation(
                            ColorAnimation(
                                colorToSet = listOf(
                                    MoveColor.ORANGE, getMoveColor(stub)
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
        val allPlayers = currentPlayingController.map { stub -> stub.macAddress }.toSet()

        if (allPlayers.size - playersLost.size == 1) {
            gameRunning = false
            val winner = (allPlayers - playersLost).first()
            playWinnerAnimationAndChangeGameState(winner)
            return
        }

        // should not happen, only if in 5ms every remaining players was defeated
        if (allPlayers == playersLost) {
            gameRunning = false
            val winner = playersLost.last()
            playWinnerAnimationAndChangeGameState(winner)
            return
        }
    }

    private suspend fun playWinnerAnimationAndChangeGameState(winner: MacAddress) {
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()
        val winnerStub = currentPlayingController.find { it.macAddress == winner }
        winnerStub?.setColorAnimation(animation = RainbowAnimation)

        val colorOfWinner = playerColors[winner]
        val colorWinsSound = colorToSound(colorOfWinner)

        colorWinsSound?.let {
            SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(id = it, minDelay = 16000L)
        }
        winnerStub?.clearAnimation()
        PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)
        delay(1000)
        GameStateManager.setGameFinished()
    }

    override suspend fun start(players: Set<PSMoveStub>) {
        initDisconnectionObserver()
        delay(100) // give lobby some time to kill all jobs
        currentPlayingController.clear()
        currentPlayingController += players
        initObservers(currentPlayingController)

        // set game sensitivity
        currentSensitivity = Settings.getSensibility()
        logger.info { "Set current Sensitivity to ${currentSensitivity.getSensibilityValues()}" }

        // set player colors
        currentPlayingController.forEachIndexed { index, player ->
            val color = listOfPlayerColors[index % listOfPlayerColors.size]
            player.setCurrentColor(color)
            playerColors[player.macAddress] = color
        }

        PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)

        currentPlayingController.forEach { player ->
            player.setColorAnimation(RainbowAnimation)
        }

        SoundManager.clearSoundQueue()
        logger.info { "play explanation..." }
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.GAME_MODE_FFA_EXPLANATION)
        logger.info { "explanation played" }

        PSMoveApi.setColor(moves = currentPlayingController.map { it.macAddress }.toSet(), color = MoveColor.RED)
        currentPlayingController.forEach { player ->
            player.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.RED, MoveColor.RED_INACTIVE
                    ), durationInMS = 1000, loop = false
                )
            )
        }
        PSMoveApi.rumble(
            moves = currentPlayingController.map { it.macAddress }.toSet(),
            intensity = RUMBLE_SOFTEST,
            durationInMs = 200
        )
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.THREE, 1000L)

        currentPlayingController.forEach { player ->
            player.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.YELLOW, MoveColor.YELLOW_INACTIVE
                    ), durationInMS = 1000, loop = false
                )
            )
        }
        PSMoveApi.rumble(
            moves = currentPlayingController.map { it.macAddress }.toSet(),
            intensity = RUMBLE_SOFT,
            durationInMs = 200
        )
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.TWO, 1000L)

        currentPlayingController.forEach { player ->
            player.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.GREEN, MoveColor.GREEN_INACTIVE
                    ), durationInMS = 1000, loop = false
                )
            )
        }
        PSMoveApi.rumble(
            moves = currentPlayingController.map { it.macAddress }.toSet(),
            intensity = RUMBLE_MEDIUM,
            durationInMs = 200
        )
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.ONE, 1000L)

        currentPlayingController.forEach { player ->
            player.setCurrentColor(colorToSet = getMoveColor(player))
        }
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.GO)

        this.gameRunning = true
        GameStateManager.setGameRunning()
    }
}