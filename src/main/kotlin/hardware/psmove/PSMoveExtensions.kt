package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.hardware.BluetoothControllerManager
import de.vanfanel.joustmania.hardware.psmove.PSMoveButton.Companion.calculatedPressedButtons
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import io.thp.psmove.Frame
import io.thp.psmove.PSMove
import kotlinx.coroutines.delay
import kotlin.math.sqrt

fun PSMove.getMacAddress(): String {
    return this._serial.uppercase()
}

suspend fun PSMove.indicatePairingComplete() {
    this.set_leds(255, 255, 255)
    this.update_leds()
    delay(3000)
    this.set_leds(0, 0, 0)
    this.update_leds()
}

fun PSMove.trust() {
    BluetoothControllerManager.trustBluetoothDevice(this.getMacAddress())
}

fun PSMove.refreshColor() {
    val color = this.currentColor
    this.set_leds(color.red, color.green, color.blue)
    this.update_leds()
}

val PSMOVE_COLOR_MAP: MutableMap<MacAddress, MoveColor> = mutableMapOf()

var PSMove.currentColor: MoveColor
    get() {
        return PSMOVE_COLOR_MAP[this.getMacAddress()] ?: MoveColor.BLACK
    }
    set(value) {
        PSMOVE_COLOR_MAP[this.getMacAddress()] = value
        this.set_leds(value.red, value.green, value.blue)
        this.update_leds()
    }


fun PSMove.pollButtons(): Set<PSMoveButton>? {
    val poll = this.poll()
    if (poll > 0) {
        val buttons = this._buttons
        val trigger = this._trigger
        return calculatedPressedButtons(buttons, trigger)
    }
    return null
}

fun PSMove.getMovingParameters(oldChange: Double): RawMovingData? {
    val poll = this.poll()
    if (poll > 0) {
        val aX = floatArrayOf(0f)
        val aY = floatArrayOf(0f)
        val aZ = floatArrayOf(0f)
        this.get_accelerometer_frame(Frame.Frame_SecondHalf, aX, aY, aZ)
        val total = sqrt((aX.first() * aX.first() + aY.first() * aY.first() + aZ.first() * aZ.first()).toDouble())
        return RawMovingData(
            accelerationX = aX.first(),
            accelerationY = aY.first(),
            accelerationZ = aZ.first(),
            total = total,
            change = (oldChange * 4 + total) / 5
        )
    }
    return null
}