package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.PSMoveBatteryLevel
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Object sends commands to the connected PSMove Controller
 *
 */
object PSMoveApi {
    private val logger = KotlinLogging.logger {}

    fun setColorOnAllMoveController(color: MoveColor) {
        logger.debug { "Setting all move controllers to $color" }
        PSMoveBluetoothConnectionWatcher.getAllMoves()
            .map { move -> setColor(macAddress = move.getMacAddress(), colorToSet = color) }
    }


    @Suppress("UNUSED")
    fun setColor(moves: Set<MacAddress>, color: MoveColor) {
        logger.debug { "Setting $moves to color: $color" }
        PSMoveBluetoothConnectionWatcher.getAllMoves().filter { moves.contains(it.getMacAddress()) }
            .map { move -> setColor(macAddress = move.getMacAddress(), colorToSet = color) }
    }

    fun getColor(macAddress: MacAddress): MoveColor? {
        return PSMoveBluetoothConnectionWatcher.getAllMoves()
            .firstOrNull { it.getMacAddress() == macAddress }?.currentColor
    }

    suspend fun refreshMoveStatus(macAddress: MacAddress): PollResult? {
        val move = PSMoveBluetoothConnectionWatcher.getMove(macAddress) ?: throw MoveNotFoundException(macAddress)
        return move.refreshMoveStatus()
    }

    fun setColor(macAddress: MacAddress, colorToSet: MoveColor) {
        try {
            PSMoveBluetoothConnectionWatcher.getMove(macAddress)?.currentColor = colorToSet
        } catch (e: Exception) {
            logger.error(e) { "Failed to set color. Reason: ${e.message}" }
        }
    }

    fun rumble(moves: Set<MacAddress>, intensity: Int, durationInMs: Long = 1000) {
        moves.forEach {
            addRumbleEvent(move = it, intensity = intensity, durationInMs =durationInMs)
        }
    }

    fun getBatteryLevel(macAddress: MacAddress): PSMoveBatteryLevel? {
        val battery: Int? = PSMoveBluetoothConnectionWatcher.getMove(macAddress)?._battery
        return PSMoveBatteryLevel.fromInt(battery)
    }

}

class MoveNotFoundException(macAddress: MacAddress) : Exception("PSMove with macAddress $macAddress not found")

const val RUMBLE_SOFTEST = 70
const val RUMBLE_SOFT = 120
const val RUMBLE_MEDIUM = 160
@Suppress("unused")
const val RUMBLE_HARD = 200
const val RUMBLE_HARDEST = 255