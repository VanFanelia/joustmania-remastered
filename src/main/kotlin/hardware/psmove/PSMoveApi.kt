package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.util.withLock
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Object sends commands to connected PSMove Controller
 *
 */
object PSMoveApi {
    private val logger = KotlinLogging.logger {}
    private val globalHardwareLock = Mutex()

    fun setColorOnAllMoveController(color: MoveColor) {
        logger.debug { "Setting all move controllers to red" }
        PSMoveBluetoothConnectionWatcher.getAllMoves().map { move -> setColor(macAddress = move.getMacAddress(), colorToSet = color) }
    }

    fun refreshColor() {
        PSMoveBluetoothConnectionWatcher.getAllMoves().map { move ->
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    withLock(globalHardwareLock) {
                        move.refreshColor()
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to set color. Reason: ${e.message}" }
            }
        }
    }

    fun pollMoveButtons(macAddress: MacAddress): Set<PSMoveButton>? {
        val move = PSMoveBluetoothConnectionWatcher.getMove(macAddress) ?: throw MoveNotFoundException(macAddress)
        return move.pollButtons()
    }

    fun pollMovement(macAddress: MacAddress, oldChange: Double): RawMovingData? {
        val move = PSMoveBluetoothConnectionWatcher.getMove(macAddress) ?: throw MoveNotFoundException(macAddress)
        return move.getMovingParameters(oldChange)
    }

    fun setColor(macAddress: MacAddress, colorToSet: MoveColor) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                withLock(globalHardwareLock) {
                    PSMoveBluetoothConnectionWatcher.getMove(macAddress)?.currentColor = colorToSet
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to set color. Reason: ${e.message}" }
        }

    }
}

class MoveNotFoundException(macAddress: MacAddress) : Exception("PSMove with macAddress $macAddress not found")
