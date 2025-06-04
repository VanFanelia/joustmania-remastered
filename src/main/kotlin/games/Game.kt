package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.hardware.psmove.PSMoveStub
import de.vanfanel.joustmania.types.MacAddress
import kotlinx.coroutines.flow.Flow

interface Game {
    val name: String
    val currentPlayingController: MutableSet<PSMoveStub>

    suspend fun start(players: Set<PSMoveStub>)

    suspend fun checkForGameFinished()

    fun cleanUpGame()

    suspend fun forceGameEnd()

    val playerLostFlow: Flow<List<MacAddress>>
}