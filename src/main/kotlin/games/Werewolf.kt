package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.GameStateManager
import de.vanfanel.joustmania.config.Settings
import de.vanfanel.joustmania.games.WerewolfFactions.VILLAGERS
import de.vanfanel.joustmania.games.WerewolfFactions.WEREWOLVES
import de.vanfanel.joustmania.hardware.PSMoveApi
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

enum class WerewolfFactions {
    WEREWOLVES,
    VILLAGERS
}

class Werewolf : GameWithAcceleration(logger = KotlinLogging.logger {}) {
    override val name = "Werewolf"
    override val currentPlayingController: MutableMap<MacAddress, PSMoveStub> = ConcurrentHashMap()
    override val minimumPlayers: Int = 3
    override val gameSelectedSound: SoundId = SoundId.GAME_MODE_WEREWOLF

    override val playerLostAnimationColors = listOf(
        MoveColor.VIOLET,
        MoveColor.VIOLET_INACTIVE,
        MoveColor.VIOLET,
        MoveColor.VIOLET_INACTIVE,
        MoveColor.BLACK
    )

    val werewolfTeam = mutableSetOf<MacAddress>()
    val villagerTeam = mutableSetOf<MacAddress>()

    override suspend fun checkForGameFinished() {
        if ((werewolfTeam - this.playersLost).isEmpty()) {
            gameRunning = false
            playWinnerAnimationAndChangeGameState(VILLAGERS)
            return
        }

        if ((villagerTeam - this.playersLost).isEmpty()) {
            gameRunning = false
            playWinnerAnimationAndChangeGameState(WEREWOLVES)
        }
    }

    private suspend fun playWinnerAnimationAndChangeGameState(winner: WerewolfFactions) {
        GameStateManager.setGameFinishing()
        SoundManager.stopSoundPlay()
        cleanUpGame()

        val winners = if (winner == WEREWOLVES) werewolfTeam else villagerTeam
        val winnerStubs: Collection<PSMoveStub> =
            currentPlayingController.filter { (key, _) -> winners.contains(key) }.values
        winnerStubs.forEach {
            it.setColorAnimation(animation = RainbowAnimation)
        }

        val winnerSound = if (winner == WEREWOLVES) SoundId.WEREWOLVES_WINS else SoundId.VILLAGERS_WINS

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = winnerSound,
            abortOnNewSound = false,
            minDelay = 16000L
        )

        winnerStubs.forEach { it.clearAnimation() }
        PSMoveApi.setColorOnAllMoveController(MoveColor.Companion.BLACK)
        delay(1000)
        GameStateManager.setGameFinished()
    }

    override fun playPlayerLostSound(macAddress: MacAddress) {
        val playerLostSound =
            if (werewolfTeam.contains(macAddress)) SoundId.A_WEREWOLF_PLAYER_LOSE else SoundId.A_VILLAGER_PLAYER_LOSE
        SoundManager.asyncAddSoundToQueue(id = playerLostSound, abortOnNewSound = false)
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

        val amountOfWerewolves: Int = getAmountOfWerewolves(players.size)
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
            currentPlayingController[address]?.setCurrentColor(MoveColor.Companion.RED_INACTIVE, true)
            currentPlayingController[address]?.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.Companion.BLACK, MoveColor.Companion.RED_INACTIVE
                    ), durationInMS = 1000, loop = false
                )
            )
        }

        villagerTeam.forEach { address ->
            currentPlayingController[address]?.setCurrentColor(MoveColor.Companion.GREEN_INACTIVE, true)
            currentPlayingController[address]?.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.Companion.BLACK, MoveColor.Companion.GREEN_INACTIVE
                    ), durationInMS = 1000, loop = false
                )
            )
        }

        delay(3000)
        currentPlayingController.forEach { (_, value) ->
            value.setCurrentColor(MoveColor.Companion.GREEN, true)
            this.setMoveColor(value.macAddress, MoveColor.Companion.GREEN)
        }

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.GAME_MODE_WEREWOLF_EXPLANATION_TWO,
            abortOnNewSound = false
        )
        delay(5000)
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.GAME_MODE_WEREWOLF_EXPLANATION_THREE,
            abortOnNewSound = false
        )
        delay(10000)
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(
            id = SoundId.GAME_MODE_WEREWOLF_EXPLANATION_FOUR,
            abortOnNewSound = false
        )
        delay(2000)

        this.gameRunning = true
        GameStateManager.setGameRunning()
    }

    private fun getAmountOfWerewolves(amountOfPlayers: Int): Int {
        val amountOfWerwolvesPerPlayers = mapOf(
            3 to arrayOf(1),
            4 to arrayOf(1),
            5 to arrayOf(1, 1, 1, 2),
            6 to arrayOf(1, 2),
            7 to arrayOf(1, 2, 2, 2),
            8 to arrayOf(2),
            9 to arrayOf(2, 2, 3),
            10 to arrayOf(2, 3),
            11 to arrayOf(2, 3, 3),
            12 to arrayOf(3),
            13 to arrayOf(3, 3, 3, 4),
            14 to arrayOf(3, 3, 4),
            15 to arrayOf(3, 4),
            16 to arrayOf(3, 4, 4),
            17 to arrayOf(3, 4, 4, 4),
            18 to arrayOf(4),
            19 to arrayOf(4, 4, 5),
        )
        val amountOfWerewolves = amountOfWerwolvesPerPlayers[amountOfPlayers] ?: arrayOf(ceil(amountOfPlayers / 5.0).toInt())
        return amountOfWerewolves.random()
    }
}