package de.vanfanel.joustmania.lobby

import de.vanfanel.joustmania.GameState
import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.games.Game
import de.vanfanel.joustmania.games.Game.Companion.gameNameToIndex
import de.vanfanel.joustmania.games.Game.Companion.gameNamesToGameObject
import de.vanfanel.joustmania.games.Game.Companion.listOfGames
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

    private val isActive: MutableMap<MacAddress, Boolean> = ConcurrentHashMap()
    private val _activeMoves: MutableStateFlow<List<MacAddress>> = MutableStateFlow(emptyList())
    val activeMoves: Flow<List<MacAddress>> = _activeMoves

    private val movesInLobby: MutableSet<MacAddress> = mutableSetOf()

    private val admins: MutableSet<MacAddress> = ConcurrentSet()
    private val _controllersWithAdminRights: MutableStateFlow<List<MacAddress>> = MutableStateFlow(emptyList())
    val controllersWithAdminRights: Flow<List<MacAddress>> = _controllersWithAdminRights

    private var freezeLobby = false

    private val lobbyJobs: MutableSet<Job> = mutableSetOf()

    private var selectedGameIndex: Int? = 0

    init {
        CoroutineScope(Dispatchers.IO).launch {
            var lastGameState: GameState? = null
            GameStateManager.currentGameState.collect { newState ->
                logger.info { "Lobby got game state: $newState" }
                if (newState == GameState.LOBBY && lastGameState != GameState.LOBBY) {
                    freezeLobby = false
                    selectedGameIndex = null
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
                isActive[moveStub.macAddress] = false
                moveStub.setNotActivatedInLobbyColor()
            }
            updateActiveMovesFlow()
            changeActiveStateOnTriggerClicked()
        })

        lobbyJobs.add(CoroutineScope(Dispatchers.IO).launch {
            changeAdminStateWhen4FrontButtonsGotClicked()
        })

        lobbyJobs.add(CoroutineScope(Dispatchers.IO).launch {
            changeGameOnSelectButtonClicked()
        })
        lobbyJobs.add(CoroutineScope(Dispatchers.IO).launch {
            changeGameOnStartButtonClicked()
        })

        lobbyJobs.add(CoroutineScope(Dispatchers.IO).launch {
            forceStartWithAllControllerWhenAdminForcedByButtonPress()
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
            if (!admins.contains(moveStub.macAddress)) {
                admins.add(moveStub.macAddress)
                soundManager.asyncAddSoundToQueue(ADMIN_GRANTED)
                logger.info { "Move with ${moveStub.macAddress} granted admin privileges" }
            } else {
                admins.remove(moveStub.macAddress)
                soundManager.asyncAddSoundToQueue(ADMIN_REVOKED)
                logger.info { "Move with ${moveStub.macAddress} lost its admin privileges" }
            }
            _controllersWithAdminRights.emit(admins.toList())

            updateLobbyColorByState()
        }
    }

    private suspend fun changeGameOnSelectButtonClicked() {
        PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
            newMoves.asFlow().flatMapMerge { move ->
                move.getSelectClickFlow.map { move }
            }
        }.collect { moveStub ->
            if (freezeLobby) return@collect
            // only admins can change the current game
            if (!admins.contains(moveStub.macAddress)) return@collect
            setCurrentGameToPreviousGameFromList()
        }
        updateActiveMovesFlow()
    }

    private suspend fun changeGameOnStartButtonClicked() {
        PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
            newMoves.asFlow().flatMapMerge { move ->
                move.getStartClickFlow.map { move }
            }
        }.collect { moveStub ->
            if (freezeLobby) return@collect
            // only admins can change the current game
            if (!admins.contains(moveStub.macAddress)) return@collect
            setCurrentGameToNextGameFromList()
        }
        updateActiveMovesFlow()
    }

    private suspend fun forceStartWithAllControllerWhenAdminForcedByButtonPress() {
        PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
            newMoves.asFlow().flatMapMerge { move ->
                move.getForceStartClickFlow.map { move }
            }
        }.collect { moveStub ->
            if (freezeLobby) return@collect
            // only admins can change the current game
            if (!admins.contains(moveStub.macAddress)) return@collect
            tryStartGame(gameMode = null, forceActivateAllController = true)
        }
        updateActiveMovesFlow()
    }

    private fun setCurrentGameToPreviousGameFromList() {
        val currentIndex = this.selectedGameIndex ?: 0
        var newIndex = (currentIndex - 1) % listOfGames.size
        if (newIndex < 0) newIndex += listOfGames.size
        this.selectedGameIndex = newIndex
        val game: Game = listOfGames[newIndex].kotlin.constructors.first().call()

        logger.info { "change current game to ${game.name}" }
        soundManager.asyncAddSoundToQueue(id = game.gameSelectedSound, abortOnNewSound = true)
    }

    private fun setCurrentGameToNextGameFromList() {
        val currentIndex = this.selectedGameIndex ?: 0
        val newIndex = (currentIndex + 1) % listOfGames.size
        this.selectedGameIndex = newIndex
        val game: Game = listOfGames[newIndex].kotlin.constructors.first().call()

        logger.info { "change current game to ${game.name}" }
        soundManager.asyncAddSoundToQueue(id = game.gameSelectedSound, abortOnNewSound = true)
    }

    fun setCurrentGameMode(gameMode: String) {
        val newIndex = gameNameToIndex[gameMode] ?: 0
        this.selectedGameIndex = newIndex
        val game: Game = listOfGames[newIndex].kotlin.constructors.first().call()

        logger.info { "change current game to ${game.name}" }
        soundManager.asyncAddSoundToQueue(id = game.gameSelectedSound, abortOnNewSound = true)
    }

    private suspend fun changeActiveStateOnTriggerClicked() {
        PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
            newMoves.asFlow().flatMapMerge { move ->
                move.getTriggerClickFlow.map { move }
            }
        }.collect { moveStub ->
            if (freezeLobby) return@collect
            if (isActive[moveStub.macAddress] == false) {
                isActive[moveStub.macAddress] = true
                updateLobbyColorByState()
                logger.info { "Move with ${moveStub.macAddress} was set to active" }
                soundManager.asyncAddSoundToQueue(CONTROLLER_JOINED)
                if (isActive.all { it.value }) {
                    tryStartGame()
                }
            } else {
                isActive[moveStub.macAddress] = false
                updateLobbyColorByState()
                soundManager.asyncAddSoundToQueue(CONTROLLER_LEFT)
                logger.info { "Move with ${moveStub.macAddress} was set to inactive" }
            }
            updateActiveMovesFlow()
        }
    }

    suspend fun tryStartGame(gameMode: String? = null, forceActivateAllController: Boolean = false) {
        val selectedGame: Game = if (gameMode !== null && gameNamesToGameObject.keys.contains(gameMode)) {
            gameNamesToGameObject[gameMode]!!.kotlin.constructors.first().call()
        } else {
            listOfGames[selectedGameIndex ?: 0].kotlin.constructors.first().call()
        }

        val activePlayers = if (forceActivateAllController) {
            isActive.keys.associateWith { true }
        } else {
            isActive.filter { isActiveEntry -> isActiveEntry.value }
        }

        if (activePlayers.size < selectedGame.minimumPlayers) {
            SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
                id = getMinimumPlayerSoundForPlayer(selectedGame.minimumPlayers),
                abortOnNewSound = false
            )
            logger.warn { "Not enough players to start the game. Minimum players needed: ${selectedGame.minimumPlayers} but only found ${activePlayers.size}" }
            return
        }
        logger.info { "All moves are ready. Start game: ${selectedGame.name}" }
        freezeLobby = true
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = ALL_PLAYERS_READY,
            abortOnNewSound = false
        )
        val players = PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.firstOrNull()
            ?.filter { isActive[it.macAddress] == true }?.toSet() ?: emptySet()
        GameStateManager.startGame(
            selectedGame, players
        )
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
                if (!newMoves.map { move -> move.macAddress }.contains(oldMove)) {
                    isActive.remove(oldMove)
                    admins.remove(oldMove)
                    _controllersWithAdminRights.emit(admins.toList())
                    soundManager.asyncAddSoundToQueue(CONTROLLER_DISCONNECTED)
                    logger.info { "Controller seems disconnecting. Remove PSMove from lobby with address: $oldMove" }
                }
            }
            updateActiveMovesFlow()
        }
    }

    private suspend fun updateActiveMovesFlow() {
        _activeMoves.emit(isActive.filter { (_, value) -> value }.map { it.key })
    }

    private suspend fun updateLobbyColorByState() {
        val stubs =
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.firstOrNull()?.associate { it.macAddress to it }
                ?: emptyMap()
        isActive.entries.forEach {
            val colorToSet = when {
                admins.contains(it.key) -> if (it.value) MoveColor.VIOLET else MoveColor.VIOLET_INACTIVE
                it.value -> MoveColor.ORANGE
                else -> MoveColor.ORANGE_INACTIVE
            }
            stubs[it.key]?.setCurrentColor(colorToSet)
        }
    }

    suspend fun handleConnectedMovesChangeDuringGameStateLobby(newMoves: Set<PSMoveStub>) {
        val newMovesMacAddresses = newMoves.map { it.macAddress }
        newMoves.forEach { newMove ->
            if (!movesInLobby.contains(newMove.macAddress)) {
                movesInLobby.add(newMove.macAddress)
                newControllerConnected(newMove)
                logger.info { "Added new PSMove controller ${newMove.macAddress} to lobby" }
            }
            movesInLobby.removeIf { it -> !newMovesMacAddresses.contains(it) }
        }
    }

    suspend fun newControllerConnected(move: PSMoveStub) {
        if (!isActive.containsKey(move.macAddress)) {
            isActive[move.macAddress] = false
        }
        move.setNotActivatedInLobbyColor()
        updateActiveMovesFlow()
        soundManager.asyncAddSoundToQueue(NEW_CONTROLLER)
    }

}