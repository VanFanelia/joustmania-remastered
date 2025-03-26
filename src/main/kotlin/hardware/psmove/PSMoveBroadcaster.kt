package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.MoveColor
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Object sends commands to every connected PSMove Controller
 */
object PSMoveBroadcaster {
    private val logger = KotlinLogging.logger {}

    fun setAllMoveControllerToRed() {
        PSMoveBluetoothConnectionWatcher.getCurrentConnectedPSMove().map { move ->
            move.currentColor = MoveColor.RED
        }
    }

    fun refreshColor() {
        PSMoveBluetoothConnectionWatcher.getCurrentConnectedPSMove().map { move ->
            move.refreshColor()
        }
    }
}

