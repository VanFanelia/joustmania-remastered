package de.vanfanel.joustmania.game

import de.vanfanel.joustmania.hardware.PSMoveBroadcaster
import de.vanfanel.joustmania.hardware.PSMovePairingManager
import de.vanfanel.joustmania.types.Ticker
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

object LobbyLoop {
    private val logger = KotlinLogging.logger {}

    private val lobbyTicker = Ticker(1.seconds)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            lobbyTicker.tick.collect {
                logger.debug { "Lobby tick tick" }
                PSMoveBroadcaster.refreshColor()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            var lastGameState: GameState? = null
            GameStateManager.currentGameState.collect { newState ->
                logger.info { "Lobby got game state: $newState" }
                if(newState == GameState.LOBBY && lastGameState != GameState.LOBBY) {
                    lobbyTicker.start()
                } else {
                    lobbyTicker.stop()
                }
                lastGameState = newState
            }
        }
    }




}