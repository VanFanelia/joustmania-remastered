package de.vanfanel.joustmania.types

import de.vanfanel.joustmania.hardware.psmove.ColorAnimation

typealias AdapterId = String
typealias MacAddress = String

data class PairedDevice(
    val adapterId: AdapterId,
    val macAddress: MacAddress,
    val name: String,
    val paired: Boolean?,
    val connected: Boolean?,
)

data class BlueToothController(
    val adapterId: AdapterId,
    val macAddress: MacAddress,
    val pairedDevices: Set<PairedDevice>,
    val pairedMotionController: Set<PairedDevice>
) {
    override fun toString(): String {
        return "$adapterId ($macAddress) - MotionController(${pairedMotionController.size}) ${pairedMotionController.map { it.macAddress }}"
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

        val ORANGE = MoveColor(210, 134, 0)
        val ORANGE_INACTIVE = MoveColor(40, 22, 0)
        val VIOLET = MoveColor(160, 32, 240)
        val VIOLET_INACTIVE = MoveColor(35, 7, 42)

        val LIGHT_BLUE = MoveColor(45, 121, 254)
        val LIGHT_BLUE_INACTIVE = MoveColor(8, 20, 55)

        val AQUA = MoveColor(0, 255, 255)
        val AQUA_INACTIVE = MoveColor(0, 35, 35)

        val MAGENTA = MoveColor(255, 0, 255)
        val MAGENTA_INACTIVE = MoveColor(35, 0, 35)


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