package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Object sends commands to connected PSMove Controller
 *
 */
object PSMoveApi {
    private val logger = KotlinLogging.logger {}

    fun setAllMoveControllerToRed() {
        logger.debug { "Setting all move controllers to red" }
        PSMoveBluetoothConnectionWatcher.getAllMoves().map { move -> move.currentColor = MoveColor.RED}
    }

    fun refreshColor() {
        PSMoveBluetoothConnectionWatcher.getAllMoves().map { move -> move.refreshColor() }
    }

    fun pollMoveButtons(macAddress: MacAddress): Set<PSMoveButton>? {
        val move = PSMoveBluetoothConnectionWatcher.getMove(macAddress) ?: throw MoveNotFoundException(macAddress)
        return move.pollButtons()
    }

    fun setColor(macAddress: MacAddress, colorToSet: MoveColor) {
        PSMoveBluetoothConnectionWatcher.getMove(macAddress)?.currentColor = colorToSet
    }
}


class MoveNotFoundException(macAddress: MacAddress) : Exception("PSMove with macAddress $macAddress not found")
