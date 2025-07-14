package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.hardware.BluetoothControllerManager
import de.vanfanel.joustmania.hardware.psmove.PSMoveButton.Companion.calculatedPressedButtons
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.thp.psmove.Frame
import io.thp.psmove.PSMove
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt


private val logger = KotlinLogging.logger {}

data class PollResult(val buttons: Set<PSMoveButton>, val movingData: RawMovingData)

fun PSMove.getMacAddress(): String {
    return this._serial.uppercase()
}

suspend fun PSMove.indicatePairingComplete() {
    try {
        this.set_leds(255, 255, 255)
        this.update_leds()
        delay(3000)
        this.set_leds(0, 0, 0)
        this.update_leds()
    } catch (e: Exception) {
        logger.warn(e) { "Failed to indicate pairing of new controller ${this.getMacAddress()}. Ignore indicating and try to continue"}
    }

}

fun PSMove.trust() {
    BluetoothControllerManager.trustBluetoothDevice(this.getMacAddress())
}

fun PSMove.refreshColor() {
    try {
        val color = this.currentColor
        this.set_leds(color.red, color.green, color.blue)
        this.update_leds()
    } catch (e: Exception) {
        logger.warn(e) { " Failed to refresh color for ${this.getMacAddress()}. Ignore color and try to continue" }
    }
}

val PSMOVE_COLOR_MAP: MutableMap<MacAddress, MoveColor> = ConcurrentHashMap()

var PSMove.currentColor: MoveColor
    get() {
        return PSMOVE_COLOR_MAP[this.getMacAddress()] ?: MoveColor.BLACK
    }
    set(value) {
        try {
            PSMOVE_COLOR_MAP[this.getMacAddress()] = value
            this.set_leds(value.red, value.green, value.blue)
            this.update_leds()
        } catch (e: Exception) {
            logger.warn(e){ "Failed to set color to ${this.getMacAddress()}. Ignore color and try to continue" }
        }

    }

val PSMOVE_OLD_CHANGE_VALUES_MAP: MutableMap<MacAddress, Double> = ConcurrentHashMap()

var PSMove.oldChange: Double
    get() {
        return PSMOVE_OLD_CHANGE_VALUES_MAP[this.getMacAddress()] ?: 0.0
    }
    set(value) {
        PSMOVE_OLD_CHANGE_VALUES_MAP[this.getMacAddress()] = value
    }

fun PSMove.pollData(): PollResult? {
    try {
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