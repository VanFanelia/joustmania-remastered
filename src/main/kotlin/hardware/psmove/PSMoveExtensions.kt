package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.hardware.BluetoothControllerManager
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import io.thp.psmove.PSMove
import kotlinx.coroutines.delay

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

fun PSMove.setNotActivatedInLobbyColor() {
        this.currentColor = MoveColor.ORANGE_INACTIVE
}

fun PSMove.refreshColor(){
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

