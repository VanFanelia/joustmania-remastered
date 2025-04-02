package de.vanfanel.joustmania.lobby

import de.vanfanel.joustmania.GameState
import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.games.FreeForAll
import de.vanfanel.joustmania.games.Game
import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.sound.SoundId.ADMIN_GRANTED
import de.vanfanel.joustmania.sound.SoundId.ADMIN_REVOKED
import de.vanfanel.joustmania.sound.SoundId.CONTROLLER_DISCONNECTED
import de.vanfanel.joustmania.sound.SoundId.CONTROLLER_JOINED
import de.vanfanel.joustmania.sound.SoundId.CONTROLLER_LEFT
import de.vanfanel.joustmania.sound.SoundId.NEW_CONTROLLER
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.Ticker
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.collections.ConcurrentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
object LobbyLoop {
    private val logger = KotlinLogging.logger {}
    private val soundManager = SoundManager

    private val lobbyTicker = Ticker(1.seconds)

    private val isActive: MutableMap<PSMoveStub, Boolean> = ConcurrentHashMap()
    private val admins: MutableSet<PSMoveStub> = ConcurrentSet()

    private val selectedGame: Game = FreeForAll()

    private val lobbyJobs: MutableSet<Job> = mutableSetOf()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            lobbyTicker.tick.collect {
                logger.debug { "Lobby tick tick" }
                PSMoveApi.refreshColor()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            var lastGameState: GameState? = null
            GameStateManager.currentGameState.collect { newState ->
                logger.info { "Lobby got game state: $newState" }
                if (newState == GameState.LOBBY && lastGameState != GameState.LOBBY) {
                    lobbyTicker.start()
                    initLobbyCoroutines()
                } else {
                    lobbyTicker.stop()
                    lobbyJobs.forEach { job -> job.cancel() }
                }
                lastGameState = newState
            }
        }

    }

    private fun initLobbyCoroutines() {
        lobbyJobs.add(CoroutineScope(Dispatchers.IO).launch {
            observeButtonPressForDebugging()
        })

        lobbyJobs.add(CoroutineScope(Dispatchers.IO).launch {
            removeControllerFromLobbyOnDisconnect()
        })

        lobbyJobs.add(CoroutineScope(Dispatchers.IO).launch {
            changeActiveStateOnTriggerClicked()
        })

        lobbyJobs.add(CoroutineScope(Dispatchers.IO).launch {
            changeAdminStateWhen4FrontButtonsGotClicked()
        })
    }

    private suspend fun observeButtonPressForDebugging() {
        PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
            newMoves.asFlow().flatMapMerge { move ->
                move.buttonPressFlow.map { buttons -> Pair(move, buttons) }
                    .onStart { logger.debug { "Move ${move.macAddress} start sending button stuff." } }
                    .onCompletion { logger.debug { "Move ${move.macAddress} stop sending button stuff." } }
            }
        }.collect {
            logger.debug { "Lobby Move: ${it.first.macAddress} has button press on buttons: ${it.second.toList()}" }
        }
    }

    private suspend fun changeAdminStateWhen4FrontButtonsGotClicked() {
        PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
            newMoves.asFlow().flatMapMerge { move ->
                move.getSquareCrossTriangleCircleClickFlow.map { move }
            }
        }.collect { moveStub ->
            if (!admins.contains(moveStub)) {
                admins.add(moveStub)
                soundManager.addSoundToQueue(ADMIN_GRANTED)
                logger.info { "Move with ${moveStub.macAddress} granted admin privileges" }
            } else {
                admins.remove(moveStub)
                soundManager.addSoundToQueue(ADMIN_REVOKED)
                logger.info { "Move with ${moveStub.macAddress} lost its admin privileges" }
            }

            updateLobbyColorByState()
        }
    }

    private suspend fun changeActiveStateOnTriggerClicked() {
        PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
            newMoves.asFlow().flatMapMerge { move ->
                move.getTriggerClickFlow.map { move }
            }
        }.collect { moveStub ->
            if (isActive[moveStub] == false) {
                isActive[moveStub] = true
                logger.info { "Move with ${moveStub.macAddress} was set to active" }
                soundManager.addSoundToQueue(CONTROLLER_JOINED)
                if (isActive.all { it.value }) {
                    logger.info { "All moves are ready. Start game: ${selectedGame.name}" }
                    GameStateManager.startGame(selectedGame)
                }
            } else {
                isActive[moveStub] = false
                soundManager.addSoundToQueue(CONTROLLER_LEFT)
                logger.info { "Move with ${moveStub.macAddress} was set to inactive" }
            }

            updateLobbyColorByState()
        }
    }

    private suspend fun removeControllerFromLobbyOnDisconnect() {
        PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.collect { newMoves ->
            isActive.keys.forEach { oldMove ->
                if (!newMoves.map { move -> move.macAddress }.contains(oldMove.macAddress)) {
                    isActive.remove(oldMove)
                    admins.remove(oldMove)
                    soundManager.addSoundToQueue(CONTROLLER_DISCONNECTED)
                    logger.info { "Controller seems disconnecting. Remove PSMove from lobby with address: $oldMove" }
                }
            }
        }
    }

    private fun updateLobbyColorByState() {
        isActive.entries.forEach {

            val colorToSet = when {
                admins.contains(it.key) -> if (it.value) MoveColor.ADMIN_BLUE_ACTIVE else MoveColor.ADMIN_BLUE_INACTIVE
                it.value -> MoveColor.ORANGE_ACTIVE
                else -> MoveColor.ORANGE_INACTIVE
            }

            it.key.setCurrentColor(colorToSet)
        }
    }

    fun newControllerConnected(move: PSMoveStub) {
        if (!isActive.containsKey(move)) {
            isActive[move] = false
        }
        move.setNotActivatedInLobbyColor()
        soundManager.addSoundToQueue(NEW_CONTROLLER)
    }


}