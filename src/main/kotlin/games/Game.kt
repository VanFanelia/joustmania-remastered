package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.types.MacAddress
import kotlinx.coroutines.flow.Flow

const val DEFAULT_GAME_NAME = "FreeForAll"

interface Game {
    val name: String
    val currentPlayingController: MutableMap<MacAddress,PSMoveStub>
    val minimumPlayers: Int
    val gameSelectedSound: SoundId

    suspend fun start(players: Set<PSMoveStub>)

    suspend fun checkForGameFinished()

    fun cleanUpGame()

    suspend fun forceGameEnd()

    val playerLostFlow: Flow<List<MacAddress>>

    companion object {
        val listOfGames: List<Class<out Game>> = listOf(
            FreeForAll::class.java as Class<out Game>,
            SortingToddler::class.java as Class<out Game>
        )

        val gameNamesToGameObject =
            listOfGames.map { return@map it.kotlin.constructors.first().call().name to it}.toMap()

        val gameNameToIndex = listOfGames.mapIndexed { index, classObject ->
            classObject.kotlin.constructors.first().call().name to index
        }.toMap()
    }
}