package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.hardware.bluetooth.BluetoothControllerManager
import de.vanfanel.joustmania.types.PSMoveButton
import de.vanfanel.joustmania.types.PSMoveButton.Companion.calculatedPressedButtons
import de.vanfanel.joustmania.types.RawMovingData
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.thp.psmove.Frame
import io.thp.psmove.PSMove
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt


/**
 * This Class Contains extensions on the PSMove class for
 */

private val logger = KotlinLogging.logger {}

/**
 * Pairing and Bluetooth trust commands
 */
suspend fun PSMove.indicatePairingComplete() {
    try {
        this.set_leds(255, 255, 255)
        this.update_leds()
        delay(3000)
        this.set_leds(0, 0, 0)
        this.update_leds()
    } catch (e: Exception) {
        logger.warn(e) { "Failed to indicate pairing of new controller ${this.getMacAddress()}. Ignore indicating and try to continue" }
    }
}

fun PSMove.trust() {
    BluetoothControllerManager.trustBluetoothDevice(this.getMacAddress())
}

/**
 * why a map here? too many accesses to this._serial create crashes
 */
val macs: MutableMap<Any, MacAddress> = ConcurrentHashMap()
fun PSMove.getMacAddress(): MacAddress {
    if (macs.containsKey(this)) {
        return macs[this] ?: this._serial.uppercase()
    } else {
        val serial = this._serial.uppercase()
        macs[this] = serial
        return serial
    }
}

/**
 * Color commands and color handling variables / maps
 */

val PSMOVE_COLOR_MAP: MutableMap<MacAddress, MoveColor> = ConcurrentHashMap()
var PSMove.currentColor: MoveColor
    get() {
        return PSMOVE_COLOR_MAP[this.getMacAddress()] ?: MoveColor.BLACK
    }
    set(value) {
        PSMOVE_COLOR_MAP[this.getMacAddress()] = value
    }

const val COLOR_UPDATE_INTERVALL_MS = 1000L
val PSMOVE_COLOR_UPDATE_MAP: MutableMap<MacAddress, Long> = ConcurrentHashMap()
val PSMOVE_LAST_COLOR_SET_COLOR_MAP: MutableMap<MacAddress, MoveColor> = ConcurrentHashMap()

val PSMove.colorUpdatedNeeded: Boolean
    get() {
        val lastUpdate = PSMOVE_COLOR_UPDATE_MAP[this.getMacAddress()] ?: return true
        val needUpdateByTime = lastUpdate + COLOR_UPDATE_INTERVALL_MS < System.currentTimeMillis()
        val newColorNeeded = PSMOVE_COLOR_MAP[this.getMacAddress()] != PSMOVE_LAST_COLOR_SET_COLOR_MAP[this.getMacAddress()]

        return needUpdateByTime || newColorNeeded
    }

private fun PSMove.refreshColor() {
    try {
        val color = this.currentColor
        this.set_leds(color.red, color.green, color.blue)
        PSMOVE_COLOR_UPDATE_MAP[this.getMacAddress()] = System.currentTimeMillis()
        PSMOVE_LAST_COLOR_SET_COLOR_MAP[this.getMacAddress()] = this.currentColor
    } catch (e: Exception) {
        logger.warn(e) { " Failed to refresh color for ${this.getMacAddress()}. Ignore color and try to continue" }
    }
}


/**
 * Rumble commands and handling
 */

sealed class RumbleCommands(val time: Long = System.currentTimeMillis()) {
    class RUMBLE(
        val intensity: Int,
        time: Long = System.currentTimeMillis()
    ) : RumbleCommands(time = time)

    class STOP(
        time: Long = System.currentTimeMillis()
    ) : RumbleCommands(time = time)
}

val PSMOVE_RUMBLE_UPDATE_MAP: MutableMap<MacAddress, List<RumbleCommands>> = ConcurrentHashMap()
private fun getRumbleCommands(move: MacAddress): List<RumbleCommands> {
    if (!PSMOVE_RUMBLE_UPDATE_MAP.containsKey(move)) {
        PSMOVE_RUMBLE_UPDATE_MAP[move] = listOf()
        return emptyList()
    }
    return PSMOVE_RUMBLE_UPDATE_MAP[move] ?: emptyList()
}

fun addRumbleEvent(move: MacAddress, intensity: Int, durationInMs: Long = 1000) {
    val newEvent = RumbleCommands.RUMBLE(intensity = intensity, time = System.currentTimeMillis())
    val stopEvent = RumbleCommands.STOP(System.currentTimeMillis() + durationInMs)
    PSMOVE_RUMBLE_UPDATE_MAP[move] = listOf(newEvent, stopEvent)
}

fun clearRumble(move: MacAddress) {
    PSMOVE_RUMBLE_UPDATE_MAP[move] = emptyList()
    PSMOVE_RUMBLE_UPDATE_MAP[move] = listOf(RumbleCommands.STOP())
}

private fun getLatestRumbleEventAndRemoveFromList(move: MacAddress): RumbleCommands? {
    val events = getRumbleCommands(move)
    if (events.isEmpty()) {
        return null
    }
    val eventsToTrigger = events.filter { it.time < System.currentTimeMillis() }
    if (eventsToTrigger.isEmpty()) return null

    // remove events that are triggered now
    PSMOVE_RUMBLE_UPDATE_MAP[move] = events - eventsToTrigger

    if (eventsToTrigger.size > 1) {
        logger.debug { "Found multiple rumble events to trigger for $move. Only use the latest one. Found: $eventsToTrigger" }
    }

    return eventsToTrigger.maxByOrNull { it.time }
}

/**
 * Functions and variables needed to poll the status of the PSMove and set rumble strength / light color
 */

val PSMOVE_OLD_CHANGE_VALUES_MAP: MutableMap<MacAddress, Double> = ConcurrentHashMap()
var PSMove.oldChange: Double
    get() {
        return PSMOVE_OLD_CHANGE_VALUES_MAP[this.getMacAddress()] ?: 0.0
    }
    set(value) {
        PSMOVE_OLD_CHANGE_VALUES_MAP[this.getMacAddress()] = value
    }

val PSMOVE_LAST_UPDATE_CALLED: MutableMap<MacAddress, Long> = ConcurrentHashMap()
var PSMove.lastUpdateCalled: Long
    get() {
        return PSMOVE_LAST_UPDATE_CALLED[this.getMacAddress()] ?: 0L
    }
    set(value) {
        PSMOVE_LAST_UPDATE_CALLED[this.getMacAddress()] = value
    }

const val UPDATE_DELAY = 5

/**
 * Polling
 */

data class PollResult(val buttons: Set<PSMoveButton>, val movingData: RawMovingData)

/**
 * Why mixing up polling and setting of the PSMove?
 *
 * To avoid memory issues, native crashes this method makes every native call called in sequence.
 * So all controller manipulations and readings are done one by one
 */
fun PSMove.refreshMoveStatus(): PollResult? {
    try {
        var callUpdate = false
        logger.trace { "[${this.getMacAddress()}] Polling data" }
        if (this.colorUpdatedNeeded) {
            logger.trace { "[${this.getMacAddress()}] color update needed" }
            refreshColor()
            callUpdate = true
            logger.trace { "[${this.getMacAddress()}] color update finished" }
        }
        val nextRumbleEvent = getLatestRumbleEventAndRemoveFromList(this.getMacAddress())
        if (nextRumbleEvent != null) {
            logger.debug { "[${this.getMacAddress()}] got rumble event: $nextRumbleEvent " }
            try {
                when (nextRumbleEvent) {
                    is RumbleCommands.RUMBLE -> {
                        logger.debug { "[${this.getMacAddress()}] rumble set to ${nextRumbleEvent.intensity}" }
                        this.set_rumble(nextRumbleEvent.intensity)
                    }

                    is RumbleCommands.STOP -> {
                        logger.debug { "[${this.getMacAddress()}] rumble set to 0" }
                        this.set_rumble(0)
                    }
                }
                callUpdate = true
            } catch (e: Exception) {
                logger.error(e) { "Failed to handle rumble event $nextRumbleEvent Reason: ${e.message}" }
            }
        }
        if (callUpdate) {
            // This updates the color and the rumble state. Only call it if a minimum of 100 ms passed
            if (this.lastUpdateCalled + UPDATE_DELAY < System.currentTimeMillis()) {
                this.update_leds()
                this.lastUpdateCalled = System.currentTimeMillis()
            }
        }
        logger.trace { "[${this.getMacAddress()}] rumble poll called" }
        val poll = this.poll()
        if (poll > 0) {
            val buttons = this._buttons
            val trigger = this._trigger
            val buttonResult = calculatedPressedButtons(buttons, trigger)

            val aX = floatArrayOf(0f)
            val aY = floatArrayOf(0f)
            val aZ = floatArrayOf(0f)
            this.get_accelerometer_frame(Frame.Frame_SecondHalf, aX, aY, aZ)
            val total = sqrt((aX.first() * aX.first() + aY.first() * aY.first() + aZ.first() * aZ.first()).toDouble())

            val newChange = (oldChange * 4 + total) / 5
            oldChange = newChange

            val movingData = RawMovingData(
                accelerationX = aX.first(),
                accelerationY = aY.first(),
                accelerationZ = aZ.first(),
                total = total,
                change = newChange
            )

            return PollResult(buttonResult, movingData)
        }
        return null
    } catch (e: Exception) {
        logger.warn(e) { " Failed to poll data for ${this.getMacAddress()}. Try to continue programm" }
        return null
    }

}