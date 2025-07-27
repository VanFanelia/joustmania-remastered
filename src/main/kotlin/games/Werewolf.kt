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
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.RainbowAnimation
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

class Werewolf : GameWithAcceleration(logger = KotlinLogging.logger {}) {
    override val name = "Werewolf"
    override val currentPlayingController: MutableMap<MacAddress, PSMoveStub> = ConcurrentHashMap()
    override val minimumPlayers: Int = 3
    override val gameSelectedSound: SoundId = SoundId.GAME_MODE_FFA

    val werewolfTeam = mutableSetOf<MacAddress>()
    val villagerTeam = mutableSetOf<MacAddress>()

    override suspend fun checkForGameFinished() {
        // TODO FIX ME
        /*
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
         */
    }

    private suspend fun playWinnerAnimationAndChangeGameState(winner: MacAddress) {
        // TODO: if villagers win play different animation / sound then when werewolfs win
        /*
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()

        val winnerStub: PSMoveStub? = currentPlayingController[winner]
        winnerStub?.setColorAnimation(animation = RainbowAnimation)

        val colorOfWinner = getMoveColor(winner)
        val colorWinsSound = SoundId.Companion.colorToSound(colorOfWinner)

        colorWinsSound?.let {
            SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
                id = it,
                abortOnNewSound = false,
                minDelay = 16000L
            )
        }
        winnerStub?.clearAnimation()
        PSMoveApi.setColorOnAllMoveController(MoveColor.Companion.BLACK)
        delay(1000)
        GameStateManager.setGameFinished()
         */
    }

    override suspend fun start(players: Set<PSMoveStub>) {
        initDisconnectionObserver()
        delay(100) // give the Lobby some time to kill all jobs
        // set game sensitivity
        currentSensitivity = Settings.getSensibility()
        logger.info { "Set current Sensitivity to ${currentSensitivity.getSensibilityValues()}" }

        currentPlayingController.clear()
        currentPlayingController.putAll(players.associateBy { it.macAddress })
        PSMoveApi.setColorOnAllMoveController(MoveColor.Companion.YELLOW)

        initObservers(currentPlayingController.keys)

        // set teams, per 5 players are one werewolf (minimum of one) rounded up
        val amountOfWerewolves: Int = ceil(players.size / 5.0).toInt()
        werewolfTeam.addAll(currentPlayingController.keys.shuffled().take(amountOfWerewolves))
        villagerTeam.addAll(currentPlayingController.keys.minus(werewolfTeam))

        SoundManager.clearSoundQueue()
        logger.info { "play explanation..." }
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.GAME_MODE_WEREWOLF_EXPLANATION,
            abortOnNewSound = false
        )
        logger.info { "explanation played" }



        currentPlayingController.forEach { player ->
            player.value.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.Companion.YELLOW, MoveColor.Companion.BLACK
                    ), durationInMS = 1000, loop = false
                )
            )
        }
        PSMoveApi.rumble(
            moves = currentPlayingController.keys,
            intensity = RUMBLE_SOFTEST,
            durationInMs = 500
        )

        delay(2000)

        werewolfTeam.forEach { address ->
            currentPlayingController[address]?.setCurrentColor(MoveColor.Companion.RED, true)
            currentPlayingController[address]?.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.Companion.BLACK, MoveColor.Companion.RED
                    ), durationInMS = 1000, loop = false
                )
            )
        }

        villagerTeam.forEach { address ->
            currentPlayingController[address]?.setCurrentColor(MoveColor.Companion.GREEN, true)
            currentPlayingController[address]?.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.Companion.BLACK, MoveColor.Companion.GREEN
                    ), durationInMS = 1000, loop = false
                )
            )
        }

        delay(3000)
        currentPlayingController.forEach { (_, value) -> value.setCurrentColor(MoveColor.Companion.GREEN, true) }

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.GAME_MODE_WEREWOLF_EXPLANATION_TWO,
            abortOnNewSound = false
        )

        delay(2000)

        this.gameRunning = true
        GameStateManager.setGameRunning()
    }
}