package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.games.FreeForAll.Companion.defaultPlayerColor
import de.vanfanel.joustmania.hardware.RUMBLE_HARDEST
import de.vanfanel.joustmania.hardware.RUMBLE_MEDIUM
import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.hardware.psmove.addRumbleEvent
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.Sensibility
import de.vanfanel.joustmania.util.CustomThreadDispatcher
import de.vanfanel.joustmania.util.onlyRemovedFromPrevious
import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

const val DEFAULT_GAME_NAME = "FreeForAll"

interface Game {
    val name: String
    val currentPlayingController: MutableMap<MacAddress, PSMoveStub>
    val minimumPlayers: Int
    val gameSelectedSound: SoundId

    suspend fun start(players: Set<PSMoveStub>)

    suspend fun checkForGameFinished()

    fun cleanUpGame()

    suspend fun forceGameEnd()

    val playersLost: MutableSet<MacAddress>
    val playerLostFlow: Flow<List<MacAddress>>

    companion object {
        val listOfGames: List<Class<out Game>> = listOf(
            FreeForAll::class.java as Class<out Game>,
            SortingToddler::class.java as Class<out Game>,
            Werewolf::class.java as Class<out Game>
        )

        val gameNamesToGameObject =
            listOfGames.map { return@map it.kotlin.constructors.first().call().name to it }.toMap()

        val gameNameToIndex = listOfGames.mapIndexed { index, classObject ->
            classObject.kotlin.constructors.first().call().name to index
        }.toMap()
    }
}

abstract class GameWithAcceleration(val logger: KLogger) : Game {
    open var disconnectedControllerJob: Job? = null

    override val playersLost: MutableSet<MacAddress> = mutableSetOf()
    private val _playerLostFlow: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    override val playerLostFlow: Flow<List<String>> = _playerLostFlow
    open val playerLostAnimationColors = listOf(
        MoveColor.RED,
        MoveColor.RED_INACTIVE,
        MoveColor.RED,
        MoveColor.RED_INACTIVE,
        MoveColor.BLACK
    )

    protected var currentSensitivity: Sensibility = Sensibility.MEDIUM

    private val playerColors: MutableMap<MacAddress, MoveColor> = mutableMapOf()
    protected fun getMoveColor(address: MacAddress): MoveColor = playerColors[address] ?: defaultPlayerColor
    protected fun setMoveColor(address: MacAddress, color: MoveColor) {
        playerColors[address] = color
    }

    private val gameJobs: MutableSet<Job> = mutableSetOf()
    protected var gameRunning: Boolean = false

    protected fun initObservers(stubs: MutableSet<MacAddress>) {
        stubs.forEach { stub ->
            gameJobs.add(observeAcceleration(stub))
        }
    }

    override fun cleanUpGame() {
        gameJobs.forEach { it.cancel("FreeForAll game go cleanup call") }
        disconnectedControllerJob?.cancel("FreeForAll game go cleanup call")
    }

    protected fun initDisconnectionObserver() {
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

    protected fun observeAcceleration(stubId: MacAddress): Job {
        val currentJob = CoroutineScope(CustomThreadDispatcher.GAME_LOOP).launch {
            val stub = currentPlayingController[stubId]
            if (stub == null) {
                return@launch
            }

            stub.accelerationFlow.collect { acceleration ->
                if (acceleration.change > 1.2 && gameRunning && !playersLost.contains(stub.macAddress)) {
                    if (acceleration.change > currentSensitivity.getSensibilityValues().deathThreshold) {
                        logger.info { "FFA: Move ${stub.macAddress} has acceleration ${acceleration.change} and lost the game" }
                        playPlayerLostSound(macAddress = stub.macAddress)
                        stub.setColorAnimation(
                            ColorAnimation(
                                colorToSet = playerLostAnimationColors, durationInMS = 3000, loop = false
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

    open fun playPlayerLostSound(macAddress: MacAddress){
        val randomSoundId = listOf(SoundId.PLAYER_LOSE_1, SoundId.PLAYER_LOSE_2).random()
        SoundManager.asyncAddSoundToQueue(id = randomSoundId, abortOnNewSound = false)
    }

    override suspend fun forceGameEnd() {
        gameRunning = false
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()
        //TODO Play game forced to stop sound
        GameStateManager.setGameFinished()
    }
}