package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.PSMoveBatteryLevel
import de.vanfanel.joustmania.util.SingleThreadDispatcher
import de.vanfanel.joustmania.util.withLock
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        logger.debug { "Setting all move controllers to $color" }
        PSMoveBluetoothConnectionWatcher.getAllMoves()
            .map { move -> setColor(macAddress = move.getMacAddress(), colorToSet = color) }
    }

    fun setColor(moves: Set<MacAddress>, color: MoveColor) {
        logger.debug { "Setting $moves to color: $color" }
        PSMoveBluetoothConnectionWatcher.getAllMoves().filter { moves.contains(it.getMacAddress()) }
            .map { move -> setColor(macAddress = move.getMacAddress(), colorToSet = color) }
    }

    fun getColor(macAddress: MacAddress): MoveColor? {
        return PSMoveBluetoothConnectionWatcher.getAllMoves()
            .firstOrNull { it.getMacAddress() == macAddress }?.currentColor
    }

    fun refreshColor() {
        PSMoveBluetoothConnectionWatcher.getAllMoves().map { move ->
            try {
                CoroutineScope(SingleThreadDispatcher.COLORS).launch {
                    withLock(globalHardwareLock) {
                        move.refreshColor()
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to set color. Reason: ${e.message}" }
            }
        }
    }

    fun pollData(macAddress: MacAddress): PollResult? {
        val move = PSMoveBluetoothConnectionWatcher.getMove(macAddress) ?: throw MoveNotFoundException(macAddress)
        return move.pollData()
    }

    fun setColor(macAddress: MacAddress, colorToSet: MoveColor) {
        try {
            CoroutineScope(SingleThreadDispatcher.COLORS).launch {
                withLock(globalHardwareLock) {
                    PSMoveBluetoothConnectionWatcher.getMove(macAddress)?.currentColor = colorToSet
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to set color. Reason: ${e.message}" }
        }
    }

    fun rumble(moves: Set<MacAddress>, intensity: Int, durationInMs: Long = 1000) {
        moves.forEach {
            rumble(it, intensity, durationInMs)
        }
    }

    fun rumble(macAddress: MacAddress, intensity: Int, durationInMs: Long = 1000) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                withLock(globalHardwareLock) {
                    PSMoveBluetoothConnectionWatcher.getMove(macAddress)?.set_rumble(intensity)
                }
                delay(durationInMs)
                withLock(globalHardwareLock) {
                    PSMoveBluetoothConnectionWatcher.getMove(macAddress)?.set_rumble(0)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to set rumble. Reason: ${e.message}" }
        }
    }

    suspend fun stopRumble(macAddress: MacAddress) {
        withLock(globalHardwareLock) {
            PSMoveBluetoothConnectionWatcher.getMove(macAddress)?.set_rumble(0)
        }
    }

    suspend fun getBatteryLevel(macAddress: MacAddress): PSMoveBatteryLevel? {
        var battery: Int? = null
        withLock(globalHardwareLock) {
            battery = PSMoveBluetoothConnectionWatcher.getMove(macAddress)?._battery
        }
        return PSMoveBatteryLevel.fromInt(battery)
    }

}

class MoveNotFoundException(macAddress: MacAddress) : Exception("PSMove with macAddress $macAddress not found")

const val RUMBLE_SOFTEST = 70
const val RUMBLE_SOFT = 120
const val RUMBLE_MEDIUM = 160
const val RUMBLE_HARD = 200
const val RUMBLE_HARDEST = 255