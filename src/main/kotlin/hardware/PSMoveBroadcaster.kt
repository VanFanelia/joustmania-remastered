package de.vanfanel.joustmania.hardware

import de.vanfanel.joustmania.types.MoveColor


object PSMoveBroadcaster {
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

