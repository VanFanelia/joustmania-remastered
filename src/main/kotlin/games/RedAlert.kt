package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.hardware.PSMoveApi
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
import de.vanfanel.joustmania.util.CustomThreadDispatcher
import de.vanfanel.joustmania.util.onlyRemovedFromPrevious
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Red Alert - Cooperative reaction game
 *
 * Rules:
 * - All controllers start GREEN
 * - Controllers randomly turn RED over time
 * - Players must SHAKE red controllers to turn them GREEN again
 * - Frequency of red alerts increases over time
 * - LOSS: More than 50% (configurable) controllers are red simultaneously
 * - WIN: Survive for minimum duration (2 min) up to max duration (5 min)
 * - Minimum 3 players required
 */
class RedAlert : Game {
    override val name = "RedAlert"
    override val currentPlayingController: MutableMap<MacAddress, PSMoveStub> = ConcurrentHashMap()
    override val minimumPlayers: Int = 3
    override val gameSelectedSound: SoundId = SoundId.GAME_MODE_RED_ALERT

    private val logger = KotlinLogging.logger {}

    // Game state
    private var gameRunning: Boolean = false
    private var gameStartTime: Long = 0

    private val redControllers = mutableSetOf<MacAddress>()
    private val warningControllers = mutableSetOf<MacAddress>() // Controllers in warning state (orange)
    private val greenControllers = mutableSetOf<MacAddress>()

    // Configuration (will be made configurable later)
    private val maxRedPercentage = 0.5 // 50% of controllers
    private val minGameDuration = 2.minutes
    private val maxGameDuration = 5.minutes

    // Difficulty scaling
    private var currentDifficultyMultiplier = 1.0
    private val difficultyIncreaseRate = 0.1 // 10% faster every interval
    private val difficultyIncreaseInterval = 15.seconds

    // Controller timing
    private val initialMinRedInterval = 10.seconds
    private val initialMaxRedInterval = 20.seconds

    // Jobs
    private val gameJobs: MutableSet<Job> = mutableSetOf()
    private var gameLoopJob: Job? = null
    private var backgroundMusicJob: Job? = null
    private var disconnectedControllerJob: Job? = null
    private var difficultyScalerJob: Job? = null

    // Player lost tracking (not used in this game mode, but required by interface)
    override val playersLost: MutableSet<MacAddress> = mutableSetOf()
    private val _playerLostFlow: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    override val playerLostFlow: Flow<List<String>> = _playerLostFlow

    override suspend fun start(players: Set<PSMoveStub>) {
        initDisconnectionObserver()
        delay(100) // give the Lobby some time to kill all jobs

        gameLoopJob = CoroutineScope(CustomThreadDispatcher.GAME_LOGIC).launch {
            PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)

            currentPlayingController.clear()
            currentPlayingController.putAll(players.associateBy { it.macAddress })

            // Initialize all controllers as green
            greenControllers.clear()
            greenControllers.addAll(currentPlayingController.keys)
            warningControllers.clear()
            redControllers.clear()

            logger.info { "Red Alert starting with ${currentPlayingController.size} players" }

            // Play explanation with gentle green pulsing animation
            SoundManager.clearSoundQueue()
            logger.info { "Playing explanation..." }

            // Start gentle green pulsing during explanation (8 color changes over 20s)
            val lightGreen = MoveColor(0, 80, 0) // Soft green
            currentPlayingController.forEach { (_, stub) ->
                stub.setColorAnimation(
                    ColorAnimation(
                        colorToSet = listOf(
                            MoveColor.BLACK,
                            lightGreen,
                            MoveColor.BLACK,
                            lightGreen,
                            MoveColor.BLACK,
                            lightGreen,
                            MoveColor.BLACK,
                            lightGreen
                        ),
                        durationInMS = 20000, // 20 seconds with 8 color changes
                        loop = false
                    )
                )
            }

            SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
                id = SoundId.GAME_MODE_RED_ALERT_EXPLANATION,
                abortOnNewSound = false
            )

            // Clear animation before countdown
            currentPlayingController.forEach { (_, stub) ->
                stub.clearAnimation()
            }

            // Countdown 3-2-1-GO
            performCountdown()

            // Set all controllers to green
            currentPlayingController.forEach { (_, stub) ->
                stub.setCurrentColor(MoveColor.GREEN)
            }

            SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
                id = SoundId.GO,
                abortOnNewSound = false
            )

            // Start game
            gameRunning = true
            gameStartTime = System.currentTimeMillis()
            GameStateManager.setGameRunning()

            // Start monitoring each controller
            currentPlayingController.keys.forEach { mac ->
                gameJobs.add(startControllerRedTimer(mac))
                gameJobs.add(startShakeMonitor(mac))
            }

            // Start difficulty scaler
            difficultyScalerJob = startDifficultyScaler()

            // Start background music
            backgroundMusicJob = playBackgroundMusic()

            logger.info { "Red Alert game started!" }
        }
    }

    /**
     * Countdown animation: 3-2-1-GO
     */
    private suspend fun performCountdown() {
        // 3 - RED
        currentPlayingController.forEach { (_, stub) ->
            stub.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(MoveColor.RED, MoveColor.RED_INACTIVE),
                    durationInMS = 1000,
                    loop = false
                )
            )
        }
        PSMoveApi.rumble(
            moves = currentPlayingController.keys,
            intensity = RUMBLE_MEDIUM,
            durationInMs = 500
        )
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.THREE,
            abortOnNewSound = false,
            minDelay = 1000L
        )

        // 2 - YELLOW
        currentPlayingController.forEach { (_, stub) ->
            stub.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(MoveColor.YELLOW, MoveColor.YELLOW_INACTIVE),
                    durationInMS = 1000,
                    loop = false
                )
            )
        }
        PSMoveApi.rumble(
            moves = currentPlayingController.keys,
            intensity = RUMBLE_MEDIUM,
            durationInMs = 500
        )
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.TWO,
            abortOnNewSound = false,
            minDelay = 1000L
        )

        // 1 - GREEN
        currentPlayingController.forEach { (_, stub) ->
            stub.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(MoveColor.GREEN, MoveColor.GREEN_INACTIVE),
                    durationInMS = 1000,
                    loop = false
                )
            )
        }
        PSMoveApi.rumble(
            moves = currentPlayingController.keys,
            intensity = RUMBLE_HARDEST,
            durationInMs = 500
        )
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.ONE,
            abortOnNewSound = false,
            minDelay = 1000L
        )
    }

    /**
     * Timer that randomly turns a controller RED
     */
    private fun startControllerRedTimer(mac: MacAddress): Job {
        return CoroutineScope(CustomThreadDispatcher.GAME_LOGIC).launch {
            while (gameRunning) {
                // Wait a random time before turning red (scaled by difficulty)
                val baseDelay = Random.nextLong(
                    initialMinRedInterval.inWholeMilliseconds,
                    initialMaxRedInterval.inWholeMilliseconds
                )
                val scaledDelay = (baseDelay / currentDifficultyMultiplier).toLong()

                // Visual warning: Animate from green to dark orange before turning red
                if (greenControllers.contains(mac) && gameRunning) {
                    val warningDuration = (scaledDelay * 0.3).toLong() // Last 30% of delay as a warning
                    val normalDelay = scaledDelay - warningDuration

                    // Wait most of the time in green
                    delay(normalDelay)

                    if (!gameRunning) break

                    // Show warning animation (green → orange → dark orange)
                    if (greenControllers.contains(mac)) {
                        // Move from green to warning state
                        greenControllers.remove(mac)
                        warningControllers.add(mac)

                        currentPlayingController[mac]?.setColorAnimation(
                            ColorAnimation(
                                colorToSet = listOf(
                                    MoveColor.GREEN,
                                    MoveColor.ORANGE,
                                    MoveColor.ORANGE_INACTIVE
                                ),
                                durationInMS = warningDuration,
                                loop = false
                            )
                        )

                        delay(warningDuration)
                    }
                }

                if (!gameRunning) break

                // Only turn red if currently in the warning state
                if (warningControllers.contains(mac)) {
                    turnControllerRed(mac)
                }
            }
        }
    }

    /**
     * Turn a controller RED
     */
    private suspend fun turnControllerRed(mac: MacAddress) {
        warningControllers.remove(mac)
        greenControllers.remove(mac)
        redControllers.add(mac)

        currentPlayingController[mac]?.setCurrentColor(MoveColor.RED)
        addRumbleEvent(move = mac, intensity = RUMBLE_MEDIUM, durationInMs = 500)

        logger.debug { "Controller $mac turned RED (${redControllers.size}/${currentPlayingController.size} red)" }

        // Check loss condition
        checkForGameFinished()
    }

    /**
     * Turn a controller GREEN (after shake from red OR warning state)
     */
    private suspend fun turnControllerGreen(mac: MacAddress) {
        redControllers.remove(mac)
        warningControllers.remove(mac)
        greenControllers.add(mac)

        // Clear any active animation and set to green
        currentPlayingController[mac]?.clearAnimation()
        currentPlayingController[mac]?.setCurrentColor(MoveColor.GREEN)
        addRumbleEvent(move = mac, intensity = RUMBLE_MEDIUM, durationInMs = 200)

        logger.debug { "Controller $mac turned GREEN (${redControllers.size}/${currentPlayingController.size} red)" }
    }

    /**
     * Monitor shake events on a controller
     */
    private fun startShakeMonitor(mac: MacAddress): Job {
        return CoroutineScope(CustomThreadDispatcher.OBSERVE_ACCELERATION).launch {
            val stub = currentPlayingController[mac] ?: return@launch

            stub.accelerationFlow.collect { acceleration ->
                // If the controller is RED or WARNING (orange) and gets shaken, turn it GREEN
                if (gameRunning && (redControllers.contains(mac) || warningControllers.contains(mac))) {
                    // Shake threshold: acceleration change > 2.0
                    if (acceleration.change > 2.0) {
                        val state = when {
                            redControllers.contains(mac) -> "RED"
                            warningControllers.contains(mac) -> "WARNING"
                            else -> "UNKNOWN"
                        }
                        logger.debug { "Controller $mac shaken from $state state (acceleration: ${acceleration.change})" }
                        turnControllerGreen(mac)
                    }
                }
            }
        }
    }

    /**
     * Increases difficulty over time
     */
    private fun startDifficultyScaler(): Job {
        return CoroutineScope(CustomThreadDispatcher.GAME_LOGIC).launch {
            while (gameRunning) {
                delay(difficultyIncreaseInterval)
                currentDifficultyMultiplier += difficultyIncreaseRate
                logger.info { "Difficulty increased! Multiplier: $currentDifficultyMultiplier" }

                // No sound for difficulty increase in cooperative mode
                // Players should focus on visual cues (controllers turning orange/red)
            }
        }
    }

    override suspend fun checkForGameFinished() {
        if (!gameRunning) return

        val totalControllers = currentPlayingController.size
        val redCount = redControllers.size
        val redPercentage = redCount.toDouble() / totalControllers

        val elapsedTime = System.currentTimeMillis() - gameStartTime
        val minDurationReached = elapsedTime >= minGameDuration.inWholeMilliseconds

        // LOSS CONDITION: Too many red controllers
        if (redPercentage > maxRedPercentage) {
            logger.info { "LOSS: $redCount/$totalControllers controllers are red (${(redPercentage * 100).toInt()}%)" }
            handleGameLoss()
            return
        }

        // WIN CONDITION: Survived minimum duration + max duration reached
        if (minDurationReached && elapsedTime >= maxGameDuration.inWholeMilliseconds) {
            logger.info { "WIN: Survived ${maxGameDuration.inWholeSeconds} seconds!" }
            handleGameWin()
            return
        }
    }

    /**
     * Handle game loss
     */
    private suspend fun handleGameLoss() {
        gameRunning = false
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()

        // Turn all controllers RED with blinking animation
        currentPlayingController.forEach { (_, stub) ->
            stub.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.RED,
                        MoveColor.BLACK,
                        MoveColor.RED,
                        MoveColor.BLACK,
                        MoveColor.RED
                    ),
                    durationInMS = 3000,
                    loop = false
                )
            )
        }

        PSMoveApi.rumble(
            moves = currentPlayingController.keys,
            intensity = RUMBLE_HARDEST,
            durationInMs = 3000
        )

        // Play loss sound
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.PLAYER_LOSE_2, // TODO: Add game over sound
            abortOnNewSound = false,
            minDelay = 3000L
        )

        PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)
        delay(1000)
        GameStateManager.setGameFinished()
    }

    /**
     * Handle game win
     */
    private suspend fun handleGameWin() {
        gameRunning = false
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()

        // Turn all controllers GREEN with rainbow celebration
        currentPlayingController.forEach { (_, stub) ->
            stub.setColorAnimation(
                de.vanfanel.joustmania.types.RainbowAnimation
            )
        }

        PSMoveApi.rumble(
            moves = currentPlayingController.keys,
            intensity = RUMBLE_MEDIUM,
            durationInMs = 2000
        )

        // Play victory sound
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.GO, // TODO: Add victory sound
            abortOnNewSound = false,
            minDelay = 3000L
        )

        PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)
        delay(1000)
        GameStateManager.setGameFinished()
    }

    override suspend fun forceGameEnd() {
        gameRunning = false
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()
        GameStateManager.setGameFinished()
    }

    override fun cleanUpGame() {
        gameJobs.forEach { it.cancel("Red Alert game ended") }
        gameJobs.clear()
        gameLoopJob?.cancel()
        backgroundMusicJob?.cancel()
        disconnectedControllerJob?.cancel()
        difficultyScalerJob?.cancel()
        SoundManager.stopBackgroundSound()
    }

    override fun playBackgroundMusic(): Job {
        return CoroutineScope(CustomThreadDispatcher.BACKGROUND_SOUND).launch {
            // TODO: Add specific Red Alert background music
            SoundManager.playBackground(soundId = SoundId.FREE_FOR_ALL_BACKGROUND_1)
        }
    }

    /**
     * Handle disconnected controllers during the game
     */
    private fun initDisconnectionObserver() {
        disconnectedControllerJob = CoroutineScope(CustomThreadDispatcher.BLUETOOTH).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.onlyRemovedFromPrevious()
                .collect { moves ->
                    moves.forEach { move ->
                        logger.warn { "Controller ${move.macAddress} disconnected during Red Alert game" }
                        greenControllers.remove(move.macAddress)
                        warningControllers.remove(move.macAddress)
                        redControllers.remove(move.macAddress)
                        currentPlayingController.remove(move.macAddress)

                        // Re-check game conditions with fewer controllers
                        checkForGameFinished()
                    }
                }
        }
    }
}
