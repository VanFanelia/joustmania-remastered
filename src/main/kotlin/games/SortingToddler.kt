package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import kotlinx.coroutines.flow.Flow

class SortingToddler: Game {
    override val name: String = "SortingToddler"
    override val currentPlayingController: MutableSet<PSMoveStub> = mutableSetOf()
    override val minimumPlayers: Int = 3
    override val gameSelectedSound: SoundId = SoundId.GAME_MODE_TODDLERS

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

        //val colorsPerPlayer = listOf<()
    }

    override suspend fun start(players: Set<PSMoveStub>) {
        TODO("Not yet implemented")
    }

    override suspend fun checkForGameFinished() {
        TODO("Not yet implemented")
    }

    override fun cleanUpGame() {
        TODO("Not yet implemented")
    }

    override suspend fun forceGameEnd() {
        TODO("Not yet implemented")
    }

    override val playerLostFlow: Flow<List<MacAddress>>
        get() = TODO("Not yet implemented")
}