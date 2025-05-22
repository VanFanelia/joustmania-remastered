package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.hardware.psmove.PSMoveStub

interface Game {
    val name: String
    val currentPlayingController: MutableSet<PSMoveStub>

    suspend fun start(players: Set<PSMoveStub>)

    suspend fun checkForGameFinished()

    fun cleanUpGame()
}