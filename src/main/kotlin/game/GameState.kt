package de.vanfanel.joustmania.game

import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


enum class GameState {
    LOBBY,
    GAME_STARTING,
    GAME_RUNNING,
    GAME_FINISHED,
    GAME_INTERRUPTED
}

object GameStateManager {

    private val logger = KotlinLogging.logger {}

    //private val currentGameState: GameState = LOBBY

    private val _currentGameState: MutableStateFlow<GameState> = MutableStateFlow(GameState.LOBBY)
    val currentGameState: Flow<GameState> = _currentGameState

    private val movesInLobby: MutableMap<String, PSMoveStub> = mutableMapOf()

    private val lobbyLoop = LobbyLoop

    init {
        CoroutineScope(Dispatchers.IO).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.collect { newMoves ->

                when (_currentGameState.value) {
                    GameState.LOBBY -> handleConnectedMovesChangeDuringGameStateLobby(newMoves)
                    GameState.GAME_STARTING -> TODO()
                    GameState.GAME_RUNNING -> TODO()
                    GameState.GAME_FINISHED -> TODO()
                    GameState.GAME_INTERRUPTED -> TODO()
                }
            }
        }
    }

    private fun handleConnectedMovesChangeDuringGameStateLobby(newMoves: Set<PSMoveStub>) {
        val newMovesMacAddresses = newMoves.map { it.macAddress }
        newMoves.forEach { newMove ->
            if (!movesInLobby.containsKey(newMove.macAddress)) {
                movesInLobby[newMove.macAddress] = newMove
                newMove.setNotActivatedInLobbyColor()
                logger.info { "Added new PSMove controller ${newMove.macAddress} to lobby" }
            }
            movesInLobby.entries.removeIf { !newMovesMacAddresses.contains(it.key) }
        }
    }

}