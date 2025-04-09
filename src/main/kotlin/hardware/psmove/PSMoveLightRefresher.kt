package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.Ticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

object PSMoveLightRefresher {

    private val lightRefreshTicker = Ticker(1.seconds)

    init {
        lightRefreshTicker.start()
        CoroutineScope(Dispatchers.IO).launch {
            lightRefreshTicker.tick.collect {
                PSMoveApi.refreshColor()
            }
        }

    }
}