package de.vanfanel.joustmania.lobby

import de.vanfanel.joustmania.GameState
import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.games.FreeForAll
import de.vanfanel.joustmania.games.Game
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundId.ADMIN_GRANTED
import de.vanfanel.joustmania.sound.SoundId.ADMIN_REVOKED
import de.vanfanel.joustmania.sound.SoundId.ALL_PLAYERS_READY
import de.vanfanel.joustmania.sound.SoundId.CONTROLLER_DISCONNECTED
import de.vanfanel.joustmania.sound.SoundId.CONTROLLER_JOINED
import de.vanfanel.joustmania.sound.SoundId.CONTROLLER_LEFT
import de.vanfanel.joustmania.sound.SoundId.NEW_CONTROLLER
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.util.collections.ConcurrentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
object LobbyLoop {
    private val logger = KotlinLogging.logger {}
    private val soundManager = SoundManager

    private val isActive: MutableMap<PSMoveStub, Boolean> = ConcurrentHashMap()
    private val _activeMoves: MutableStateFlow<List<MacAddress>> = MutableStateFlow(emptyList())
    val activeMoves: Flow<List<MacAddress>> = _activeMoves

    private val movesInLobby: MutableMap<String, PSMoveStub> = mutableMapOf()

    private val admins: MutableSet<PSMoveStub> = ConcurrentSet()
    private val _controllersWithAdminRights: MutableStateFlow<List<MacAddress>> = MutableStateFlow(emptyList())
    val controllersWithAdminRights: Flow<List<MacAddress>> = _controllersWithAdminRights

    private var selectedGame: Game? = null
    private var freezeLobby = false

    private val lobbyJobs: MutableSet<Job> = mutableSetOf()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            var lastGameState: GameState? = null
            GameStateManager.currentGameState.collect { newState ->
                logger.info { "Lobby got game state: $newState" }
                if (newState == GameState.LOBBY && lastGameState != GameState.LOBBY) {
                    freezeLobby = false
                    selectedGame = null
                    isActive.clear()
                    _activeMoves.emit(emptyList())
                    initLobbyCoroutines()
                } else {
                    lobbyJobs.forEach { job -> job.cancel() }
                    admins.clear()
                    _controllersWithAdminRights.emit(emptyList())
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
            val connectedPSMoves = PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.firstOrNull()
            connectedPSMoves?.forEach { moveStub ->
                isActive[moveStub] = false
                moveStub.setNotActivatedInLobbyColor()
            }
            updateActiveMovesFlow()
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
                soundManager.asyncAddSoundToQueue(ADMIN_GRANTED)
                logger.info { "Move with ${moveStub.macAddress} granted admin privileges" }
            } else {
                admins.remove(moveStub)
                soundManager.asyncAddSoundToQueue(ADMIN_REVOKED)
                logger.info { "Move with ${moveStub.macAddress} lost its admin privileges" }
            }
            _controllersWithAdminRights.emit(admins.map { it.macAddress })

            updateLobbyColorByState()
        }
    }

    private suspend fun changeActiveStateOnTriggerClicked() {
        PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
            newMoves.asFlow().flatMapMerge { move ->
                move.getTriggerClickFlow.map { move }
            }
        }.collect { moveStub ->
            if (freezeLobby) return@collect
            if (isActive[moveStub] == false) {
                isActive[moveStub] = true
                updateLobbyColorByState()
                logger.info { "Move with ${moveStub.macAddress} was set to active" }
                soundManager.asyncAddSoundToQueue(CONTROLLER_JOINED)
                if (isActive.all { it.value }) {
                    tryStartGame()
                }
            } else {
                isActive[moveStub] = false
                updateLobbyColorByState()
                soundManager.asyncAddSoundToQueue(CONTROLLER_LEFT)
                logger.info { "Move with ${moveStub.macAddress} was set to inactive" }
            }
            updateActiveMovesFlow()
        }
    }

    suspend fun tryStartGame() {
        if (selectedGame == null) {
            selectedGame = FreeForAll()
        }

        selectedGame?.let { game ->
            val activePlayers = isActive.filter { isActiveEntry -> isActiveEntry.value }
            if (activePlayers.size < game.minimumPlayers) {
                SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
                    id = getMinimumPlayerSoundForPlayer(game.minimumPlayers),
                    abortOnNewSound = false
                )
                logger.warn { "Not enough players to start the game. Minimum players needed: ${game.minimumPlayers} but only found ${activePlayers.size}" }
                return
            }
            logger.info { "All moves are ready. Start game: ${game.name}" }
            freezeLobby = true
            SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
                id = ALL_PLAYERS_READY,
                abortOnNewSound = false
            )
            GameStateManager.startGame(
                game, activePlayers.keys
            )
        }
    }

    private fun getMinimumPlayerSoundForPlayer(minimumPlayers: Int): SoundId {
        if (minimumPlayers == 3) {
            return SoundId.MINIMUM_PLAYERS_3
        }
        return SoundId.MINIMUM_PLAYERS_2
    }

    private suspend fun removeControllerFromLobbyOnDisconnect() {
        PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.collect { newMoves ->
            isActive.keys.forEach { oldMove ->
                if (!newMoves.map { move -> move.macAddress }.contains(oldMove.macAddress)) {
                    isActive.remove(oldMove)
                    admins.remove(oldMove)
                    _controllersWithAdminRights.emit(admins.map { it.macAddress })
                    soundManager.asyncAddSoundToQueue(CONTROLLER_DISCONNECTED)
                    logger.info { "Controller seems disconnecting. Remove PSMove from lobby with address: $oldMove" }
                }
            }
            updateActiveMovesFlow()
        }
    }

    private suspend fun updateActiveMovesFlow() {
        _activeMoves.emit(isActive.filter { (_, value) -> value }.map { it.key.macAddress })
    }

    private fun updateLobbyColorByState() {
        isActive.entries.forEach {
            val colorToSet = when {
                admins.contains(it.key) -> if (it.value) MoveColor.VIOLET else MoveColor.VIOLET_INACTIVE
                it.value -> MoveColor.ORANGE
                else -> MoveColor.ORANGE_INACTIVE
            }

            it.key.setCurrentColor(colorToSet)
        }
    }

    suspend fun handleConnectedMovesChangeDuringGameStateLobby(newMoves: Set<PSMoveStub>) {
        val newMovesMacAddresses = newMoves.map { it.macAddress }
        newMoves.forEach { newMove ->
            if (!movesInLobby.containsKey(newMove.macAddress)) {
                movesInLobby[newMove.macAddress] = newMove
                newControllerConnected(newMove)
                logger.info { "Added new PSMove controller ${newMove.macAddress} to lobby" }
            }
            movesInLobby.entries.removeIf { !newMovesMacAddresses.contains(it.key) }
        }
    }

    suspend fun newControllerConnected(move: PSMoveStub) {
        if (!isActive.containsKey(move)) {
            isActive[move] = false
        }
        move.setNotActivatedInLobbyColor()
        updateActiveMovesFlow()
        soundManager.asyncAddSoundToQueue(NEW_CONTROLLER)
    }


}