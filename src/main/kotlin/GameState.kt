package de.vanfanel.joustmania

import de.vanfanel.joustmania.games.Game
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.lobby.LobbyLoop
import de.vanfanel.joustmania.types.Ticker
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


enum class GameState {
    LOBBY,
    GAME_STARTING,
    GAME_RUNNING,
    GAME_FINISHING,
    GAME_FINISHED,
    GAME_INTERRUPTED
}

object GameStateManager {

    private val logger = KotlinLogging.logger {}

    private val _currentGameState: MutableStateFlow<GameState> = MutableStateFlow(GameState.LOBBY)
    val currentGameState: Flow<GameState> = _currentGameState

    // Todo: Move to lobby?
    private val movesInLobby: MutableMap<String, PSMoveStub> = mutableMapOf()

    private val lobbyLoop = LobbyLoop

    private var currentGame: Game? = null
    private val gameWatcherTicker = Ticker(5.milliseconds)
    private var gameWatcherJob: Job? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            // TODO: move changes of connected Bluetooth to Lobby and game!
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.collect { newMoves ->
                if (_currentGameState.value == GameState.LOBBY) {
                    handleConnectedMovesChangeDuringGameStateLobby(newMoves)
                }
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            currentGameState.collect { newState ->
                when (newState) {
                    GameState.LOBBY -> gameModeChangedToLobby()
                    GameState.GAME_STARTING -> handleGameStarting()
                    GameState.GAME_RUNNING -> handleGameStarted()
                    GameState.GAME_FINISHING -> handleGameFinishing()
                    GameState.GAME_FINISHED -> handleGameFinished()
                    GameState.GAME_INTERRUPTED -> handleGameInterrupted()
                }
            }
        }
    }

    private fun handleConnectedMovesChangeDuringGameStateLobby(newMoves: Set<PSMoveStub>) {
        val newMovesMacAddresses = newMoves.map { it.macAddress }
        newMoves.forEach { newMove ->
            if (!movesInLobby.containsKey(newMove.macAddress)) {
                movesInLobby[newMove.macAddress] = newMove
                lobbyLoop.newControllerConnected(newMove)
                logger.info { "Added new PSMove controller ${newMove.macAddress} to lobby" }
            }
            movesInLobby.entries.removeIf { !newMovesMacAddresses.contains(it.key) }
        }
    }

    private fun gameModeChangedToLobby() {
        logger.info { "Game mode changed to lobby" }
    }

    private fun handleGameStarting() {
        logger.info { "Game is starting..." }
    }

    private fun handleGameStarted() {
        logger.info { "Game has started." }
        gameWatcherTicker.start()

        gameWatcherJob = CoroutineScope(Dispatchers.IO).launch {
            gameWatcherTicker.tick.collect {
                currentGame?.checkForGameFinished()
            }
        }
    }

    private fun handleGameFinishing() {
        gameWatcherTicker.stop()
        logger.info { "Game has finished." }
    }

    private fun handleGameFinished() {
        currentGame = null
        gameWatcherJob?.cancel()
        CoroutineScope(Dispatchers.Default).launch {
            _currentGameState.emit(GameState.LOBBY)
        }
    }

    private fun handleGameInterrupted() {
        logger.info { "Game has interrupted." }
    }

    suspend fun startGame(game: Game, players: Set<PSMoveStub>) {
        currentGame = game
        val currentGameState = _currentGameState.value
        if (currentGameState == GameState.LOBBY) {
            CoroutineScope(Dispatchers.Default).launch {
                game.start(players = players)
            }
            _currentGameState.emit(GameState.GAME_STARTING)
        } else {
            logger.warn { "Cannot start game while another game is running" }
        }
    }

    suspend fun setGameRunning() {
        _currentGameState.emit(GameState.GAME_RUNNING)
    }

    suspend fun setGameFinishing() {
        _currentGameState.emit(GameState.GAME_FINISHING)
    }

    suspend fun setGameFinished() {
        _currentGameState.emit(GameState.GAME_FINISHED)
    }

}