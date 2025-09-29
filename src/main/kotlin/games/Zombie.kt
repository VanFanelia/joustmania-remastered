package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.hardware.PSMoveApi
import de.vanfanel.joustmania.hardware.RUMBLE_HARD
import de.vanfanel.joustmania.hardware.RUMBLE_HARDEST
import de.vanfanel.joustmania.hardware.RUMBLE_MEDIUM
import de.vanfanel.joustmania.hardware.RUMBLE_SOFT
import de.vanfanel.joustmania.hardware.RUMBLE_SOFTEST
import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.hardware.psmove.addRumbleEvent
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.sound.SoundManager.playBackground
import de.vanfanel.joustmania.sound.SoundManager.stopBackgroundSound
import de.vanfanel.joustmania.sound.SoundManager.stopSoundPlay
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.Sensibility
import de.vanfanel.joustmania.util.CustomThreadDispatcher
import de.vanfanel.joustmania.util.onlyRemovedFromPrevious
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.engine.launchOnCancellation
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

class Zombie : Game {

    val logger = KotlinLogging.logger {}

    override val name = "Zombie"
    override val currentPlayingController: MutableMap<MacAddress, PSMoveStub> = ConcurrentHashMap()
    override val minimumPlayers: Int = 3
    val maxGameDurationInSeconds = 60 * 3
    var zombieOutTimeInMs: Long = 5000
    var lastZombieTimeOut: Long = 0;
    val zombieTimeoutDelay: Long = zombieOutTimeInMs * 2
    var gameStartet: Long? = null

    val playerLostAnimationColors = listOf(
        MoveColor.GREEN,
        MoveColor.GREEN_INACTIVE,
        MoveColor.BLACK,
        MoveColor.VIOLET_INACTIVE,
        MoveColor.VIOLET,
    )

    override val gameSelectedSound: SoundId = SoundId.GAME_MODE_ZOMBIE

    // Lost Players = Zombie
    override val playersLost: MutableSet<MacAddress> = mutableSetOf()
    private val _playerLostFlow: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    override val playerLostFlow: Flow<List<MacAddress>> = MutableStateFlow(emptyList())

    val zombieTeam = playersLost

    // ready players
    val readyPlayers = mutableMapOf<MacAddress, Boolean>()

    var isZombieOutTime = false

    private var backgroundMusicJob: Job? = null
    private var triggerButtonObserverJob: Job? = null
    private var moveButtonObserverJob: Job? = null

    var currentHumanSensibility: Sensibility = Sensibility.VERY_HIGH
    var currentZombieSensibility: Sensibility = Sensibility.LOW


    companion object {
        val humanNotReadyColor = MoveColor.GREEN_INACTIVE
        val humanReadyColor = MoveColor.GREEN
        val zombieNotReadyColor = MoveColor.VIOLET_INACTIVE
        val zombieReadyColor = MoveColor.VIOLET

        fun getColorForPlayer(isReady: Boolean, isZombie: Boolean): MoveColor {
            if (isZombie) {
                return if (isReady) {
                    zombieReadyColor
                } else {
                    zombieNotReadyColor
                }
            }
            return if (isReady) {
                humanReadyColor
            } else {
                humanNotReadyColor
            }
        }

        fun getMinAmountOfZombies(amountOfPlayers: Int): Int {
            return ceil((amountOfPlayers + 1.0) / 10.0).toInt()
        }

        fun getMaxAmountOfZombies(amountOfPlayers: Int): Int {
            val maxZombies = mapOf(
                1 to 1,
                2 to 1,
                3 to 1,
                4 to 1,
                5 to 1,
                6 to 2,
                7 to 2,
                8 to 2,
                9 to 2,
            )
            return maxZombies[amountOfPlayers] ?: ceil((amountOfPlayers + 1.0) / 5.0).toInt()
        }
    }

    @OptIn(InternalAPI::class)
    override fun playBackgroundMusic(): Job {
        return CoroutineScope(CustomThreadDispatcher.BACKGROUND_SOUND).launch {
            val sound = arrayOf(SoundId.WEREWOLF_BACKGROUND_1, SoundId.WEREWOLF_BACKGROUND_2).random()
            playBackground(sound)
        }.launchOnCancellation {
            stopBackgroundSound()
        }
    }

    private val gameJobs: MutableSet<Job> = mutableSetOf()
    private var disconnectedControllerJob: Job? = null
    private var gameRunning: Boolean = false

    private fun initObservers(stubs: MutableSet<MacAddress>) {
        stubs.forEach { stub ->
            gameJobs.add(observeAcceleration(stub))
        }
    }

    private fun initDisconnectionObserver() {
        disconnectedControllerJob = CoroutineScope(CustomThreadDispatcher.BLUETOOTH).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.onlyRemovedFromPrevious().collect { moves ->
                moves.forEach { move ->
                    logger.info { "Move with address: ${move.macAddress} was disconnected. Set Player to game Over" }
                    playersLost.add(move.macAddress)
                    _playerLostFlow.emit(playersLost.toList())
                }
            }
        }
    }

    override suspend fun start(players: Set<PSMoveStub>) {
        initDisconnectionObserver()
        delay(100) // give the Lobby some time to kill all jobs

        // set game sensitivity
        // TODO ADD NEW SETTINGS FOR ZOMBIE: HUMANS_SENSIBILITY And Zombie game duration and zombie OutTime
        //currentSensitivity = Settings.getSensibility()
        //logger.info { "Set current Sensitivity to ${currentSensitivity.getSensibilityValues()}" }

        currentPlayingController.clear()
        currentPlayingController.putAll(players.associateBy { it.macAddress })
        readyPlayers.putAll(players.map { it.macAddress to false })

        // Start the Zombie Lobby by explaining the game
        zombieLobby()

        // If all Players checked in via the trigger button, start the game and start Observing acceleration
        initObservers(currentPlayingController.keys)
    }

    private suspend fun zombieLobby() {
        logger.info { "Start Zombie lobby" }

        PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)
        PSMoveApi.setColor(currentPlayingController.keys, humanNotReadyColor)

        // pick random zombies
        val defaultZombies = getMinAmountOfZombies(currentPlayingController.size)

        val pickedZombies = mutableListOf<MacAddress>()
        val survivors = currentPlayingController.keys.toMutableSet()

        // to avoid a null pointer here, we check sizes
        if (survivors.isNotEmpty()) {
            repeat(defaultZombies) {
                val zombie = survivors.random()
                pickedZombies.add(zombie)
                survivors.remove(zombie)
            }
        }

        zombieTeam.addAll(pickedZombies)

        PSMoveApi.setColor(moves = zombieTeam, color = zombieNotReadyColor)

        SoundManager.clearSoundQueue()
        logger.info { "play zombie lobby explanation in background..." }
        SoundManager.asyncAddSoundToQueue(
            id = SoundId.GAME_MODE_ZOMBIE_LOBBY_EXPLANATION, abortOnNewSound = false
        )

        initButtonObserver()

        // Wait until all Players have checked in via the trigger button
        while (readyPlayers.values.any { !it } || !isThereAZombie() || currentAmountOfZombies() > getMaxAmountOfZombies(
                currentPlayingController.size
            )) {

            if (currentAmountOfZombies() > getMaxAmountOfZombies(currentPlayingController.size)) {
                SoundManager.asyncAddSoundToQueue(
                    id = SoundId.GAME_MODE_ZOMBIE_TOO_MANY_ZOMBIES, abortOnNewSound = false
                )
            }

            delay(1000)
        }
        removeButtonObserver()

        SoundManager.clearSoundQueue()
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.ALL_PLAYERS_READY, abortOnNewSound = false
        )
        delay(1000)

        PSMoveApi.rumble(
            moves = currentPlayingController.keys, intensity = RUMBLE_SOFTEST, durationInMs = 500
        )

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.THREE, abortOnNewSound = false, minDelay = 1000L
        )

        PSMoveApi.rumble(
            moves = currentPlayingController.keys, intensity = RUMBLE_MEDIUM, durationInMs = 500
        )

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.TWO, abortOnNewSound = false, minDelay = 1000L
        )

        PSMoveApi.rumble(
            moves = currentPlayingController.keys, intensity = RUMBLE_HARDEST, durationInMs = 500
        )

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.ONE, abortOnNewSound = false, minDelay = 1000L
        )

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(id = SoundId.GO, abortOnNewSound = false)

        gameStartet = System.currentTimeMillis()
        gameRunning = true
        backgroundMusicJob = playBackgroundMusic()
        GameStateManager.setGameRunning()
    }

    private fun isThereAZombie(): Boolean {
        return zombieTeam.any { true }
    }

    private fun currentAmountOfZombies(): Int = zombieTeam.size

    private fun isHuman(macAddress: MacAddress) = macAddress !in zombieTeam

    private fun isZombie(macAddress: MacAddress) = macAddress in zombieTeam

    private fun observeAcceleration(stubId: MacAddress): Job {
        return CoroutineScope(CustomThreadDispatcher.OBSERVE_ACCELERATION).launch {
            val stub = currentPlayingController[stubId] ?: return@launch

            stub.accelerationFlow.collect { acceleration ->
                if (acceleration.change > 1.2 && gameRunning) {

                    if (isHuman(stub.macAddress)) {
                        if (acceleration.change > currentHumanSensibility.getSensibilityValues().deathThreshold && !isZombieOutTime) {
                            logger.info { "Zombie Game: The Human player Move ${stub.macAddress} has acceleration ${acceleration.change} and is now a zombie" }
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
                            stub.setCurrentColor(colorToSet = zombieReadyColor)
                        } else if (acceleration.change > currentHumanSensibility.getSensibilityValues().warningThreshold) {
                            logger.info { "Zombie Game: Move ${stub.macAddress} has acceleration ${acceleration.change} and got a warning" }
                            addRumbleEvent(move = stub.macAddress, intensity = RUMBLE_MEDIUM, durationInMs = 1000)
                            stub.setColorAnimation(
                                ColorAnimation(
                                    colorToSet = listOf(
                                        MoveColor.YELLOW, humanReadyColor
                                    ), durationInMS = 1000, loop = false
                                )
                            )
                        }
                    }

                    if (isZombie(stub.macAddress) && acceleration.change > currentZombieSensibility.getSensibilityValues().deathThreshold && !isZombieOutTime) {
                        val now = System.currentTimeMillis()
                        if (now - zombieTimeoutDelay < lastZombieTimeOut) {
                            logger.info { "The last Zombie timeout was ${now - lastZombieTimeOut} ms ago. So no new timeout is needed. Source of timeout: ${stub.macAddress}" }
                            return@collect
                        }
                        isZombieOutTime = true

                        lastZombieTimeOut = now
                        logger.info { "Zombie Game: The Zombie player Move ${stub.macAddress} has acceleration ${acceleration.change} so all zombies got a timeout" }
                        // TODO play Zombie gurgle sound
                        val zombieOutTimeColorAnimation = ColorAnimation(
                            colorToSet = listOf(
                                zombieReadyColor,
                                zombieNotReadyColor,
                                MoveColor.BLACK,
                                zombieNotReadyColor,
                                MoveColor.BLACK,
                                zombieNotReadyColor,
                                zombieReadyColor
                            ), durationInMS = zombieOutTimeInMs, loop = false
                        )
                        val zombieStubs = currentPlayingController.filter { it.key in zombieTeam }.values

                        zombieStubs.forEach { it.setColorAnimation(zombieOutTimeColorAnimation) }
                        val rumbleTime = zombieOutTimeInMs / 5
                        PSMoveApi.rumble(moves = zombieTeam, intensity = RUMBLE_HARDEST, durationInMs = rumbleTime)
                        delay(rumbleTime + 1)
                        PSMoveApi.rumble(moves = zombieTeam, intensity = RUMBLE_HARD, durationInMs = rumbleTime)
                        delay(rumbleTime + 1)
                        PSMoveApi.rumble(moves = zombieTeam, intensity = RUMBLE_MEDIUM, durationInMs = rumbleTime)
                        delay(rumbleTime + 1)
                        PSMoveApi.rumble(moves = zombieTeam, intensity = RUMBLE_SOFT, durationInMs = rumbleTime)
                        delay(rumbleTime + 1)
                        PSMoveApi.rumble(moves = zombieTeam, intensity = RUMBLE_SOFTEST, durationInMs = rumbleTime)
                        delay(rumbleTime + 1)

                        logger.info { "Zombie Game: Zombie Out Time finished. All Zombies can play normal now" }
                        isZombieOutTime = false
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initButtonObserver() {
        moveButtonObserverJob = CoroutineScope(CustomThreadDispatcher.GAME_CONTROLLER_ACTION).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
                newMoves.asFlow().flatMapMerge { move ->
                    move.getMoveClickFlow.map { move }
                }
            }.collect { moveStub ->
                // only the current playing moves need to be handled
                if (moveStub.macAddress !in currentPlayingController.keys) {
                    return@collect
                }
                val maxZombie = getMaxAmountOfZombies(currentPlayingController.size)
                val currentZombie = playersLost.size

                if (moveStub.macAddress in zombieTeam) {
                    // remove vom zombie
                    zombieTeam.remove(moveStub.macAddress)

                    PSMoveApi.setColor(
                        moveStub.macAddress, getColorForPlayer(readyPlayers[moveStub.macAddress] ?: false, false)
                    )

                    logger.info { "Player ${moveStub.macAddress} was removed from the Zombie Team" }
                    return@collect
                }

                if (currentZombie < maxZombie) {
                    zombieTeam.add(moveStub.macAddress)
                    PSMoveApi.setColor(
                        moveStub.macAddress, getColorForPlayer(readyPlayers[moveStub.macAddress] ?: false, true)
                    )

                    logger.info { "Player ${moveStub.macAddress} was added to the Zombie Team" }
                    return@collect
                } else {
                    //play to many zombies error-sound, but only if no other sound is played
                    SoundManager.asyncAddSoundToQueueIfQueueIsEmpty(
                        id = SoundId.GAME_MODE_ZOMBIE_TOO_MANY_ZOMBIES, abortOnNewSound = false
                    )
                }
            }
        }
        triggerButtonObserverJob = CoroutineScope(CustomThreadDispatcher.GAME_CONTROLLER_ACTION).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
                newMoves.asFlow().flatMapMerge { move ->
                    move.getTriggerClickFlow.map { move }
                }
            }.collect { moveStub ->
                // only the current playing moves need to be handled
                if (moveStub.macAddress !in currentPlayingController.keys) {
                    return@collect
                }
                val isReady: Boolean = readyPlayers[moveStub.macAddress] ?: return@collect
                val newReadyState = !isReady
                readyPlayers[moveStub.macAddress] = newReadyState

                PSMoveApi.setColor(
                    moveStub.macAddress, getColorForPlayer(newReadyState, moveStub.macAddress in playersLost)
                )
                logger.info { "Player ${moveStub.macAddress} changed ready state to: ${if (newReadyState) "ready" else "not ready"}" }
            }
        }
    }

    private fun removeButtonObserver() {
        moveButtonObserverJob?.cancel("Game start now")
        moveButtonObserverJob = null
        triggerButtonObserverJob?.cancel("Game start now")
        triggerButtonObserverJob = null
    }

    override suspend fun checkForGameFinished() {
        if (!gameRunning) return
        if (System.currentTimeMillis() - (gameStartet ?: Long.MAX_VALUE) > maxGameDurationInSeconds * 1000) {
            gameRunning = false
            stopSoundPlay()
            stopBackgroundSound()
            cleanUpGame()
            playHumansWinAnimationAndChangeGameState()
            return
        }
        if (zombieTeam == currentPlayingController.keys) {
            gameRunning = false
            stopSoundPlay()
            stopBackgroundSound()
            cleanUpGame()
            playZombieWinAnimationAndChangeGameState()
        }
    }

    val winAnimationDuration = 5000L

    private suspend fun playHumansWinAnimationAndChangeGameState() {
        logger.info { "The Humans win the Zombie-Game" }

        // Wait for player death animation and sound to finish
        delay(4000)

        SoundManager.asyncAddSoundToQueue(
            id = SoundId.GAME_MODE_ZOMBIE_HUMAN_WINS, abortOnNewSound = false
        )

        val humanWinColorAnimation = ColorAnimation(
            colorToSet = listOf(
                humanReadyColor,
                humanNotReadyColor,
                humanReadyColor,
                humanNotReadyColor,
                humanReadyColor
            ), durationInMS = winAnimationDuration, loop = false
        )
        currentPlayingController.values.forEach { it.setColorAnimation(humanWinColorAnimation) }

        val rumbleTime = winAnimationDuration / 5L
        PSMoveApi.rumble(moves = currentPlayingController.keys, intensity = RUMBLE_HARDEST, durationInMs = rumbleTime)
        delay(rumbleTime + 1)
        PSMoveApi.rumble(moves = currentPlayingController.keys, intensity = RUMBLE_MEDIUM, durationInMs = rumbleTime)
        delay(rumbleTime + 1)
        PSMoveApi.rumble(moves = currentPlayingController.keys, intensity = RUMBLE_HARDEST, durationInMs = rumbleTime)
        delay(rumbleTime + 1)
        PSMoveApi.rumble(moves = currentPlayingController.keys, intensity = RUMBLE_MEDIUM, durationInMs = rumbleTime)
        delay(rumbleTime + 1)
        PSMoveApi.rumble(moves = currentPlayingController.keys, intensity = RUMBLE_SOFTEST, durationInMs = rumbleTime)
        delay(rumbleTime + 1)

        GameStateManager.setGameFinished()
    }

    private suspend fun playZombieWinAnimationAndChangeGameState() {
        logger.info { "The Zombies win the Zombie-Game" }

        // Wait for player death animation and sound to finish
        delay(4000)

        SoundManager.asyncAddSoundToQueue(
            id = SoundId.GAME_MODE_ZOMBIE_ZOMBIES_WINS, abortOnNewSound = false
        )

        val zombiesWinsAnimation = ColorAnimation(
            colorToSet = listOf(
                zombieReadyColor,
                zombieNotReadyColor,
                zombieReadyColor,
                zombieNotReadyColor,
                zombieReadyColor
            ), durationInMS = winAnimationDuration, loop = false
        )
        currentPlayingController.values.forEach { it.setColorAnimation(zombiesWinsAnimation) }

        val rumbleTime = winAnimationDuration / 5L
        PSMoveApi.rumble(moves = currentPlayingController.keys, intensity = RUMBLE_HARDEST, durationInMs = rumbleTime)
        delay(rumbleTime + 1)
        PSMoveApi.rumble(moves = currentPlayingController.keys, intensity = RUMBLE_HARD, durationInMs = rumbleTime)
        delay(rumbleTime + 1)
        PSMoveApi.rumble(moves = currentPlayingController.keys, intensity = RUMBLE_MEDIUM, durationInMs = rumbleTime)
        delay(rumbleTime + 1)
        PSMoveApi.rumble(moves = currentPlayingController.keys, intensity = RUMBLE_SOFT, durationInMs = rumbleTime)
        delay(rumbleTime + 1)
        PSMoveApi.rumble(moves = currentPlayingController.keys, intensity = RUMBLE_SOFTEST, durationInMs = rumbleTime)
        delay(rumbleTime + 1)

        GameStateManager.setGameFinished()
    }

    fun playPlayerLostSound(macAddress: MacAddress) {
        // only humans can be eliminated
        if (macAddress !in zombieTeam) {
            SoundManager.asyncAddSoundToQueue(
                id = SoundId.GAME_MODE_ZOMBIE_A_PERSON_WAS_BITTEN_BY_ZOMBIE, abortOnNewSound = true
            )
        }
    }

    override fun cleanUpGame() {
        backgroundMusicJob?.cancel("Zombie game finished. Music needs to stop.")
        moveButtonObserverJob?.cancel("Game ended")
        triggerButtonObserverJob?.cancel("Game ended")
        stopBackgroundSound()
        gameJobs.forEach { it.cancel("FreeForAll game go cleanup call") }
        disconnectedControllerJob?.cancel("FreeForAll game go cleanup call")
    }

    override suspend fun forceGameEnd() {
        gameRunning = false
        GameStateManager.setGameFinishing()
        stopSoundPlay()
        stopBackgroundSound()
        cleanUpGame()
        GameStateManager.setGameFinished()
    }
}