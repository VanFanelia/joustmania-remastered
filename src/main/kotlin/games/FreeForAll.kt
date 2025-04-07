package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.RainbowAnimation
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay

class FreeForAll : Game {
    private val logger = KotlinLogging.logger {}

    override val name = "FreeForAll"
    override val currentPlayingController: MutableSet<PSMoveStub> = mutableSetOf()

    override suspend fun startGameStart(players: Set<PSMoveStub>) {
        delay(100) // give lobby some time to kill all jobs
        currentPlayingController.clear()
        currentPlayingController += players

        PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)

        currentPlayingController.forEach { player ->
            player.setColorAnimation(RainbowAnimation)
        }

        SoundManager.clearSoundQueue()
        logger.info { "play explanation..." }
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.GAME_MODE_FFA_EXPLANATION)
        logger.info { "explanation played" }

        PSMoveApi.setColorOnAllMoveController(MoveColor.RED)
        currentPlayingController.forEach { player ->
            player.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.RED, MoveColor.RED_INACTIVE
                    ), durationInMS = 1000, loop = false
                )
            )
        }
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.THREE, 1000L)

        PSMoveApi.setColorOnAllMoveController(MoveColor.RED)
        currentPlayingController.forEach { player ->
            player.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.YELLOW, MoveColor.YELLOW_INACTIVE
                    ), durationInMS = 1000, loop = false
                )
            )
        }
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.TWO, 1000L)

        currentPlayingController.forEach { player ->
            player.setColorAnimation(
                ColorAnimation(
                    colorToSet = listOf(
                        MoveColor.GREEN, MoveColor.GREEN_INACTIVE
                    ), durationInMS = 1000, loop = false
                )
            )
        }
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.ONE, 1000L)

        currentPlayingController.forEach { player ->
            player.setCurrentColor(MoveColor.MAGENTA)
        }
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.GO)

    }
}