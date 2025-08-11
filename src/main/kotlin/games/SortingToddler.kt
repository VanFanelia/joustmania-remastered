package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.config.Settings
import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import de.vanfanel.joustmania.hardware.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.hardware.RUMBLE_SOFT
import de.vanfanel.joustmania.hardware.psmove.addRumbleEvent
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.sound.SoundManager.playBackground
import de.vanfanel.joustmania.sound.SoundManager.stopBackgroundSound
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.darken
import de.vanfanel.joustmania.util.onlyRemovedFromPrevious
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.engine.launchOnCancellation
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class SortingToddler : Game {
    private val logger = KotlinLogging.logger {}

    override val name: String = "SortingToddler"

    override val currentPlayingController: MutableMap<MacAddress, PSMoveStub> = ConcurrentHashMap()
    override val minimumPlayers: Int = 3
    override val gameSelectedSound: SoundId = SoundId.GAME_MODE_TODDLERS

    override val playersLost: MutableSet<MacAddress> = emptySet<MacAddress>().toMutableSet()
    override val playerLostFlow: Flow<List<MacAddress>>
        get() = flowOf(emptyList())

    private var connectedControllerChangeJob: Job? = null
    private var disconnectedControllerJob: Job? = null
    private var backgroundMusicJob: Job? = null
    private var numberOfPlayersOnGameStart: Int = 3

    private var maxRounds = 10
    private var roundLength = 30
    private var gameHasEnded = false
    private var currentColorConfiguration: Map<MacAddress, MoveColor> = emptyMap()

    private var gameLoopJob: Job? = null

    companion object {
        val gameColors = listOf(
            MoveColor.RED,
            MoveColor.GREEN,
            MoveColor.BLUE,
            MoveColor.YELLOW,
            MoveColor.ORANGE,
            MoveColor.MAGENTA,
            MoveColor.AQUA,
            MoveColor.WHITE,
        )

        val colorsPerPlayer = mapOf(
            1 to 1,
            2 to 1,
            3 to 2,
            4 to 2,
            5 to 2,
            6 to 3,
            7 to 3,
            8 to 3,
            9 to 3,
            10 to 3,
            11 to 4,
            12 to 4,
            13 to 4,
            14 to 4,
            15 to 4,
            16 to 4,
        )

        fun getColorsForPlayer(player: Int): Int {
            if (colorsPerPlayer.containsKey(player)) {
                return colorsPerPlayer[player] ?: 5
            }
            return 5
        }

    }

    private fun initControllerConnectedObserver() {
        connectedControllerChangeJob = CoroutineScope(Dispatchers.IO).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.collect { moves ->
                logger.info { "Connected PSMoves change during game. Detected ${moves.size} connected PSMoves. Update game" }
                currentPlayingController.clear()
                moves.forEach { move ->
                    logger.info { "Moves changed with address: ${move.macAddress} connected. add player to game" }
                    currentPlayingController[move.macAddress] = move
                }
            }
        }
    }

    private fun initDisconnectionObserver() {
        disconnectedControllerJob = CoroutineScope(Dispatchers.IO).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.onlyRemovedFromPrevious().collect { moves ->
                moves.forEach { move ->
                    logger.info { "Move with address: ${move.macAddress} was disconnected. remove player from game" }
                    currentPlayingController.remove(move.macAddress)
                }
            }
        }
    }

    override suspend fun start(players: Set<PSMoveStub>) {
        initControllerConnectedObserver()
        initDisconnectionObserver()
        val config = Settings.currentConfig
        maxRounds = config.sortToddlerGameOptions.amountOfRounds
        roundLength = config.sortToddlerGameOptions.roundDuration

        delay(100) // give the Lobby some time to kill all jobs
        currentPlayingController.clear()
        currentPlayingController.putAll(players.associateBy { it.macAddress })
        numberOfPlayersOnGameStart = players.size

        SoundManager.clearSoundQueue()
        logger.info { "play explanation..." }
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.GAME_MODE_TODDLER_EXPLANATION,
            abortOnNewSound = false
        )
        logger.info { "explanation played" }

        gameLoopJob = CoroutineScope(Dispatchers.IO).launch {
            for (round in 1..maxRounds) {
                logger.info { "Round $round/$maxRounds in toddler game with ${currentPlayingController.size}" }
                val colorsNeeded = getColorsForPlayer(currentPlayingController.size)
                val colors = gameColors.shuffled().take(colorsNeeded)
                val shuffledPlayers = currentPlayingController.keys.shuffled()
                val colorsOfRound = shuffledPlayers
                    .mapIndexed { index, player ->
                        player to colors[index % colors.size]
                    }.toMap()
                // set new colors
                currentColorConfiguration = colorsOfRound
                colorsOfRound.forEach { (player, color) ->
                    logger.info { "Give color $color to player $player" }
                    PSMoveApi.setColor(player, color)
                }

                delay((roundLength * 1000L) - 3000L)
                // warn for change
                currentPlayingController.map { stub ->
                    val stubColor = currentColorConfiguration[stub.key] ?: MoveColor.WHITE
                    stub.value.setColorAnimation(
                        ColorAnimation(
                            colorToSet = listOf(
                                stubColor,
                                stubColor.darken(),
                                stubColor,
                                stubColor.darken(),
                                MoveColor.BLACK
                            ),
                            durationInMS = 3000,
                            loop = false,
                        )
                    )
                    addRumbleEvent(move = stub.key, intensity = RUMBLE_SOFT, durationInMs = 3000)
                }

                delay(4000L)
                if (round == maxRounds) {
                    gameHasEnded = true
                }
            }
        }

        GameStateManager.setGameRunning()
        backgroundMusicJob = playBackgroundMusic()
    }

    @OptIn(InternalAPI::class)
    override fun playBackgroundMusic(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            val sound = arrayOf(SoundId.SORT_TODDLER_BACKGROUND_1).random()
            playBackground(sound)
        }.launchOnCancellation {
            stopBackgroundSound()
        }
    }

    override suspend fun checkForGameFinished() {
        if (!gameHasEnded) return

        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()

        PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)
        delay(1000)
        GameStateManager.setGameFinished()
    }

    override fun cleanUpGame() {
        gameLoopJob?.cancel()
        currentPlayingController.forEach {
            it.value.clearAnimation()
        }

        disconnectedControllerJob?.cancel("SortingToddler game go cleanup call")
        backgroundMusicJob?.cancel("SortingToddler game go cleanup call")
        stopBackgroundSound()
    }

    override suspend fun forceGameEnd() {
        gameHasEnded = true
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()
        GameStateManager.setGameFinished()
    }
}