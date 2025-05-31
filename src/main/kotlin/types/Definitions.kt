package de.vanfanel.joustmania.types

import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import kotlinx.serialization.Serializable

typealias AdapterId = String
typealias MacAddress = String

@Serializable
data class PairedDevice(
    val adapterId: AdapterId,
    val macAddress: MacAddress,
    val name: String,
    val paired: Boolean?,
    val connected: Boolean?,
)

@Serializable
data class BlueToothController(
    val adapterId: AdapterId,
    val macAddress: MacAddress,
    val name: String,
    val pairedDevices: Set<PairedDevice>,
    val pairedMotionController: Set<PairedDevice>
) {
    override fun toString(): String {
        return "$adapterId ($macAddress) - MotionController(${pairedMotionController.size}) ${pairedMotionController.map { it.macAddress }}"
    }
}

@Serializable
data class BlueToothControllerStats(
    val adapterId: AdapterId,
    val macAddress: MacAddress,
    val name: String,
    val pairedMotionController: Set<MotionControllerStats>
)

@Serializable
data class MotionControllerStats(
    val adapterId: AdapterId,
    val macAddress: MacAddress,
    val connected: Boolean?,
    val isAdmin: Boolean?,
    val batteryLevel: Int?,
)

@Serializable
data class GameStats(
    val currentGameState: String,
    val activeController: List<MacAddress>
)

enum class PSMoveBatteryLevel(val value: Int) {
    LEVEL_0(0),              // leer / minimal
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3),
    LEVEL_4(4),
    LEVEL_5(5),              // voll
    CHARGING(6),             // wird geladen
    CHARGING_DONE(7);        // Ladevorgang abgeschlossen

    companion object {
        fun fromInt(value: Int?): PSMoveBatteryLevel? {
            return entries.find { it.value == value }
        }
    }
}

data class BluetoothInfo(
    val bus: String,
    val device: String,
    val id: String,
    val nameWithType: String,
)

data class MoveColor(
    val red: Int,
    val green: Int,
    val blue: Int
) {
    companion object {
        val BLACK = MoveColor(0, 0, 0)
        val RED = MoveColor(255, 0, 0)
        val RED_INACTIVE = MoveColor(55, 0, 0)
        val GREEN = MoveColor(0, 255, 0)
        val GREEN_INACTIVE = MoveColor(0, 22, 0)
        val BLUE = MoveColor(0, 0, 255)
        val BLUE_INACTIVE = MoveColor(0, 0, 63)
        val YELLOW = MoveColor(255, 255, 0)
        val YELLOW_INACTIVE = MoveColor(35, 35, 0)

        val LIGHT_BLUE = MoveColor(45, 121, 254)
        val LIGHT_BLUE_INACTIVE = MoveColor(8, 20, 55)

        val AQUA = MoveColor(0, 255, 255)
        val AQUA_INACTIVE = MoveColor(0, 35, 35)

        val MAGENTA = MoveColor(255, 0, 255)
        val MAGENTA_INACTIVE = MoveColor(35, 0, 35)

        val WHITE = MoveColor(255, 255, 255)
        val WHITE_INACTIVE = MoveColor(45, 45, 45)

        val ORANGE = MoveColor(255, 128, 0)
        val ORANGE_INACTIVE = MoveColor(40, 22, 0)

        val VIOLET = MoveColor(127, 0, 255)
        val VIOLET_INACTIVE = MoveColor(20, 0, 42)

        val LIME = MoveColor(128, 255, 0)
        val LIME_INACTIVE = MoveColor(20, 42, 0)

        val PINK = MoveColor(255, 153, 153)
        val PINK_INACTIVE = MoveColor(45, 32, 32)

        /* to near to aqua */
        val PASTEL_GREEN = MoveColor(0, 255, 128)
        val PASTEL_GREEN_INACTIVE = MoveColor(0, 45, 30)

        fun getColorByName(name: String?): MoveColor {
            return when (name) {
                "BLACK" -> BLACK
                "RED" -> RED
                "RED_INACTIVE" -> RED_INACTIVE
                "GREEN" -> GREEN
                "GREEN_INACTIVE" -> GREEN_INACTIVE
                "BLUE" -> BLUE
                "BLUE_INACTIVE" -> BLUE_INACTIVE
                "YELLOW" -> YELLOW
                "YELLOW_INACTIVE" -> YELLOW_INACTIVE

                "ORANGE" -> ORANGE
                "ORANGE_INACTIVE" -> ORANGE_INACTIVE
                "VIOLET" -> VIOLET
                "VIOLET_INACTIVE" -> VIOLET_INACTIVE

                "LIGHT_BLUE" -> LIGHT_BLUE
                "LIGHT_BLUE_INACTIVE" -> LIGHT_BLUE_INACTIVE

                "AQUA" -> AQUA
                "AQUA_INACTIVE" -> AQUA_INACTIVE

                "MAGENTA" -> MAGENTA
                "MAGENTA_INACTIVE" -> MAGENTA_INACTIVE

                "WHITE" -> WHITE
                "WHITE_INACTIVE" -> WHITE_INACTIVE

                "LIME" -> LIME
                "LIME_INACTIVE" -> LIME_INACTIVE

                "PINK" -> PINK
                "PINK_INACTIVE" -> PINK_INACTIVE

                "PASTEL_GREEN" -> PASTEL_GREEN
                "PASTEL_GREEN_INACTIVE" -> PASTEL_GREEN_INACTIVE
                else -> {
                    BLACK
                }
            }
        }
    }

    override fun toString(): String {
        return "MoveColor(red=${red}, green=${green}, blue=${blue})"
    }
}

val RainbowAnimation = ColorAnimation(colorToSet = listOf(
    MoveColor.RED,
    MoveColor.YELLOW,
    MoveColor.GREEN,
    MoveColor.AQUA,
    MoveColor.BLUE,
    MoveColor.MAGENTA
), durationInMS = 8000)