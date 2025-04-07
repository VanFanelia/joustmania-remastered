package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.RainbowAnimation
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay

class FreeForAll: Game {
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

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.THREE)

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.TWO)

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.ONE)

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.GO)

    }
}