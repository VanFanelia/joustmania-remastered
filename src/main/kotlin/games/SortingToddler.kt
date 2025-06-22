package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.hardware.psmove.RUMBLE_SOFT
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.darken
import de.vanfanel.joustmania.util.onlyRemovedFromPrevious
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class SortingToddler : Game {
    private val logger = KotlinLogging.logger {}

    override val name: String = "SortingToddler"
    override val currentPlayingController: MutableSet<PSMoveStub> = mutableSetOf()
    override val minimumPlayers: Int = 3
    override val gameSelectedSound: SoundId = SoundId.GAME_MODE_TODDLERS

    private var disconnectedControllerJob: Job? = null
    private var numberOfPlayersOnGameStart: Int = 3

    // change toddler game mode length per settings
    private val gameLengthInMilliseconds = 5 * 60 * 1000L
    private val maxRounds = 10
    private val roundLength = gameLengthInMilliseconds / maxRounds
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

    private fun initDisconnectionObserver() {
        disconnectedControllerJob = CoroutineScope(Dispatchers.IO).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.onlyRemovedFromPrevious().collect { moves ->
                moves.forEach { move ->
                    logger.info { "Move with address: ${move.macAddress} was disconnected. remove player from game" }
                    currentPlayingController.removeIf { it.macAddress == move.macAddress }
                }
            }
        }
    }

    override suspend fun start(players: Set<PSMoveStub>) {
        initDisconnectionObserver()
        delay(100) // give the Lobby some time to kill all jobs
        currentPlayingController.clear()
        currentPlayingController += players
        numberOfPlayersOnGameStart = players.size

        gameLoopJob = CoroutineScope(Dispatchers.IO).launch {
            for (round in 1..maxRounds) {
                logger.info { "Round $round/$maxRounds in toddler game with ${currentPlayingController.size}" }
                val colorsNeeded = getColorsForPlayer(currentPlayingController.size)
                val colors = gameColors.shuffled().take(colorsNeeded)
                val shuffledPlayers = currentPlayingController.map { it.macAddress }.shuffled()
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
                delay(roundLength - 3000)
                // warn for change

                currentPlayingController.map { stub ->
                    val stubColor = currentColorConfiguration[stub.macAddress] ?: MoveColor.WHITE
                    stub.setColorAnimation(
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
                    PSMoveApi.rumble(macAddress = stub.macAddress, intensity = RUMBLE_SOFT, durationInMs = 3000)
                }
                delay(4000)
                if (round == maxRounds) {
                    gameHasEnded = true
                }
            }

        }

        GameStateManager.setGameRunning()
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
        currentPlayingController.map { stub ->
            stub.clearAnimation()
        }
        disconnectedControllerJob?.cancel("FreeForAll game go cleanup call")
    }

    override suspend fun forceGameEnd() {
        gameHasEnded = true
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()
        // TODO Play game forced to stop sound
        GameStateManager.setGameFinished()
    }

    override val playerLostFlow: Flow<List<MacAddress>>
        get() = flowOf(emptyList())
}