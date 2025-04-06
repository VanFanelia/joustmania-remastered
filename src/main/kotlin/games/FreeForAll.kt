package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.hardware.BluetoothCommands
import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MoveColor
import kotlinx.coroutines.delay

class FreeForAll: Game {

    override val name = "FreeForAll"
    override val currentPlayingController: MutableSet<PSMoveStub> = mutableSetOf()

    override suspend fun startGameStart(players: Set<PSMoveStub>) {
        delay(100) // give lobby some time to kill all jobs
        currentPlayingController.clear()
        currentPlayingController += players

        PSMoveApi.setColorOnAllMoveController(MoveColor.BLACK)

        /*
        currentPlayingController.forEach {
            it.setCurrentColor()
        }

        PSMoveApi.setBlinkColorToController(players, listOf<MoveColor>(MoveColor.ORANGE_INACTIVE, MoveColor.ORANGE_INACTIVE))

        SoundManager.clearSoundQueue()
        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.GAME_FFA_INTRODUCTION)

        SoundManager.addSoundToQueueAndWaitForPlayerFinishedThisSound(SoundId.Gam)

         */
    }
}