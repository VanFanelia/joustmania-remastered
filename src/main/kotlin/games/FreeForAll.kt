package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.config.Settings
import de.vanfanel.joustmania.hardware.PSMoveApi
import de.vanfanel.joustmania.hardware.RUMBLE_HARDEST
import de.vanfanel.joustmania.hardware.RUMBLE_MEDIUM
import de.vanfanel.joustmania.hardware.RUMBLE_SOFTEST
import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundId.Companion.colorToSound
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.sound.SoundManager.playBackground
import de.vanfanel.joustmania.sound.SoundManager.stopBackgroundSound
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.RainbowAnimation
import de.vanfanel.joustmania.util.CustomThreadDispatcher
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.engine.launchOnCancellation
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class FreeForAll : GameWithAcceleration(logger = KotlinLogging.logger {}) {
    override val name = "FreeForAll"
    override val currentPlayingController: MutableMap<MacAddress, PSMoveStub> = ConcurrentHashMap()
    override val minimumPlayers: Int = 2
    override val gameSelectedSound: SoundId = SoundId.GAME_MODE_FFA

    private var backgroundMusicJob: Job? = null
    private var gameLoopJob: Job? = null

    companion object {
        val listOfPlayerColors = listOf(
            MoveColor.GREEN,
        )

        val defaultPlayerColor = MoveColor.WHITE
    }

    override suspend fun checkForGameFinished() {
        val allPlayers = currentPlayingController.keys

        if (allPlayers.size - playersLost.size == 1) {
            gameRunning = false
            val winner = (allPlayers - playersLost).first()
            playWinnerAnimationAndChangeGameState(winner)
            return
        }

        // should not happen, only if inside 5 ms every remaining player was defeated
        if (allPlayers == playersLost) {
            gameRunning = false
            val winner = playersLost.last()
            playWinnerAnimationAndChangeGameState(winner)
            return
        }
    }

    private suspend fun playWinnerAnimationAndChangeGameState(winner: MacAddress) {
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()

        val winnerStub: PSMoveStub? = currentPlayingController[winner]
        winnerStub?.setColorAnimation(animation = RainbowAnimation)

        val colorOfWinner = getMoveColor(winner)
        val colorWinsSound = colorToSound(colorOfWinner)

        colorWinsSound?.let {
            SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
                id = it,
                abortOnNewSound = false,
                minDelay = 16000L
            )
        }
        winnerStub?.clearAnimation()
        PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)
        delay(1000)
        GameStateManager.setGameFinished()
    }

    override suspend fun start(players: Set<PSMoveStub>) {
        initDisconnectionObserver()
        delay(100) // give the Lobby some time to kill all jobs

        gameLoopJob = CoroutineScope(CustomThreadDispatcher.GAME_LOGIC).launch {
            // set game sensitivity
            currentSensitivity = Settings.getSensibility()
            logger.info { "Set current Sensitivity to ${currentSensitivity.getSensibilityValues()}" }

            PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)

            currentPlayingController.clear()
            currentPlayingController.putAll(players.associateBy { it.macAddress })
            initObservers(currentPlayingController.keys)

            // set player colors
            currentPlayingController.onEachIndexed { index, player ->
                val color = listOfPlayerColors[index % listOfPlayerColors.size]
                player.value.setCurrentColor(color)
                setMoveColor(player.key, color)
            }

            currentPlayingController.forEach { player ->
                player.value.setColorAnimation(RainbowAnimation)
            }

            SoundManager.clearSoundQueue()
            logger.info { "play explanation..." }
            SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
                id = SoundId.GAME_MODE_FFA_EXPLANATION,
                abortOnNewSound = false
            )
            logger.info { "explanation played" }


            currentPlayingController.forEach { player -> player.value.setCurrentColor(MoveColor.RED) }
            currentPlayingController.forEach { player ->
                player.value.setColorAnimation(
                    ColorAnimation(
                        colorToSet = listOf(
                            MoveColor.RED, MoveColor.RED_INACTIVE
                        ), durationInMS = 1000, loop = false
                    )
                )
            }
            PSMoveApi.rumble(
                moves = currentPlayingController.keys,
                intensity = RUMBLE_SOFTEST,
                durationInMs = 500
            )


            SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
                id = SoundId.THREE,
                abortOnNewSound = false,
                minDelay = 1000L
            )

            currentPlayingController.forEach { player ->
                player.value.setColorAnimation(
                    ColorAnimation(
                        colorToSet = listOf(
                            MoveColor.YELLOW, MoveColor.YELLOW_INACTIVE
                        ), durationInMS = 1000, loop = false
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


            currentPlayingController.forEach { player ->
                player.value.setColorAnimation(
                    ColorAnimation(
                        colorToSet = listOf(
                            MoveColor.GREEN, MoveColor.GREEN_INACTIVE
                        ), durationInMS = 1000, loop = false
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

            currentPlayingController.forEach { player ->
                player.value.setCurrentColor(colorToSet = getMoveColor(player.key))
            }

            SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(id = SoundId.GO, abortOnNewSound = false)

            gameRunning = true
            backgroundMusicJob = playBackgroundMusic()
            GameStateManager.setGameRunning()

        }
    }

    @OptIn(InternalAPI::class)
    override fun playBackgroundMusic(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            val sound = arrayOf(SoundId.FREE_FOR_ALL_BACKGROUND_1).random()
            playBackground(sound)
        }.launchOnCancellation {
            stopBackgroundSound()
        }
    }

    override fun cleanUpGame() {
        backgroundMusicJob?.cancel("Free For All game ended so background music need to stop")
        stopBackgroundSound()
        gameLoopJob?.cancel()
        super.cleanUpGame()
    }
}