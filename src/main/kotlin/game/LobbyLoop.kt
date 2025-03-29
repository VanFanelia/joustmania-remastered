package de.vanfanel.joustmania.game

import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.Ticker
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val lobbyTicker = Ticker(1.seconds)

    private val isActive: MutableMap<PSMoveStub, Boolean> = ConcurrentHashMap()
    private val isAdmin: MutableMap<PSMoveStub, Boolean> = ConcurrentHashMap()

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
                } else {
                    lobbyTicker.stop()
                }
                lastGameState = newState
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            // for debug: Observe pressed state
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

        CoroutineScope(Dispatchers.IO).launch {
            removeControllerFromLobbyOnDisconnect()
        }

        CoroutineScope(Dispatchers.IO).launch {
            changeActiveStateOnTriggerClicked()
        }

        CoroutineScope(Dispatchers.IO).launch {
            changeAdminStateWhen4FrontButtonsGotClicked()
        }
    }

    private suspend fun changeAdminStateWhen4FrontButtonsGotClicked() {
        PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
            newMoves.asFlow().flatMapMerge { move ->
                move.getSquareCrossTriangleCircleClickFlow.map { move }
            }
        }.collect { moveStub ->
            if (!isAdmin.containsKey(moveStub)) {
                isAdmin[moveStub] = true
                logger.info { "Move with ${moveStub.macAddress} granted admin privileges" }
            } else {
                isAdmin.remove(moveStub)
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
            if (!isActive.containsKey(moveStub)) {
                isActive[moveStub] = true
                logger.info { "Move with ${moveStub.macAddress} was set to active" }
            } else {
                isActive[moveStub] = !isActive[moveStub]!!
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
                    isAdmin.remove(oldMove)
                    logger.info { "Controller seems disconnecting. Remove PSMove from lobby with address: $oldMove" }
                }
            }
        }
    }

    private fun updateLobbyColorByState() {
        isActive.entries.forEach {

            val colorToSet = when {
                isAdmin.containsKey(it.key) -> if (it.value) MoveColor.ADMIN_BLUE_ACTIVE else MoveColor.ADMIN_BLUE_INACTIVE
                it.value -> MoveColor.ORANGE_ACTIVE
                else -> MoveColor.ORANGE_INACTIVE
            }

            it.key.setCurrentColor(colorToSet)
        }
    }


}