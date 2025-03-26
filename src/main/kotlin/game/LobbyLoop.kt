package de.vanfanel.joustmania.game

import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.hardware.psmove.currentColor
import de.vanfanel.joustmania.hardware.psmove.getMacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.Ticker
import io.github.oshai.kotlinlogging.KotlinLogging
import io.thp.psmove.PSMove
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
                logger.info { "Lobby Move: ${it.first.macAddress} has button press on buttons: ${it.second.toList()}" }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.collect { newMoves ->
                isActive.keys.forEach { oldMove ->
                    // check if move get lost
                    if (!newMoves.map { move -> move.macAddress }.contains(oldMove.macAddress)) {
                        isActive.remove(oldMove)
                    }
                }
            }
        }


        CoroutineScope(Dispatchers.IO).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
                newMoves.asFlow().flatMapMerge { move ->
                    move.getTriggerClickFlow.map { move }
                }
            }.collect {
                if (!isActive.containsKey(it)) {
                    isActive[it] = true
                } else {
                    isActive[it] = ! isActive[it]!!
                }
                logger.info { "Click happend to state: $it" }
                updateActiveColors()
            }

        }
    }

    private fun updateActiveColors() {
        isActive.entries.forEach {
            val colorToSet = if(it.value) MoveColor.ORANGE_ACTIVE else MoveColor.ORANGE_INACTIVE
            it.key.setCurrentColor(colorToSet)
        }
    }


}