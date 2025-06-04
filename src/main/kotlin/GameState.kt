package de.vanfanel.joustmania

import de.vanfanel.joustmania.games.Game
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.lobby.LobbyLoop
import de.vanfanel.joustmania.types.MacAddress
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
    GAME_INTERRUPTED;

    fun toDisplayString(): String {
        return when (this) {
            GAME_STARTING -> "Starting"
            LOBBY -> "Lobby"
            GAME_RUNNING -> "Running"
            GAME_FINISHING -> "Finishing"
            GAME_FINISHED -> "Finished"
            GAME_INTERRUPTED -> "Interrupted"
        }
    }
}

object GameStateManager {

    private val logger = KotlinLogging.logger {}

    private val _currentGameState: MutableStateFlow<GameState> = MutableStateFlow(GameState.LOBBY)
    val currentGameState: Flow<GameState> = _currentGameState

    private val lobbyLoop = LobbyLoop

    private var currentGame: Game? = null
    private val gameWatcherTicker = Ticker(5.milliseconds)
    private var gameWatcherJob: Job? = null

    private val _playerLostFlow = MutableStateFlow<Set<MacAddress>>(emptySet())
    val playerLostFlow: Flow<Set<MacAddress>> = _playerLostFlow
    private var playerLostJob: Job? = null

    private var playersInGame: Set<MacAddress> = emptySet()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            // TODO: move changes of connected Bluetooth to Lobby and game!
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.collect { newMoves ->
                if (_currentGameState.value == GameState.LOBBY) {
                    lobbyLoop.handleConnectedMovesChangeDuringGameStateLobby(newMoves)
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
        playersInGame = emptySet()
        gameWatcherJob?.cancel()
        playerLostJob?.cancel()
        CoroutineScope(Dispatchers.Default).launch {
            _currentGameState.emit(GameState.LOBBY)
            _playerLostFlow.emit(emptySet())
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
                playersInGame = players.map { it.macAddress }.toSet()
                game.start(players = players)
            }

            playerLostJob = CoroutineScope(Dispatchers.IO).launch {
                game.playerLostFlow.collect { playerLost ->
                    _playerLostFlow.emit(playerLost.toSet())
                }
            }

            _currentGameState.emit(GameState.GAME_STARTING)

        } else {
            logger.warn { "Cannot start game while another game is running" }
        }
    }

    suspend fun forceStopGame() {
        currentGame?.forceGameEnd()
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

    fun getPlayerInGameList(): Set<MacAddress> {
        return playersInGame
    }

}