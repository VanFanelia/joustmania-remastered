package de.vanfanel.joustmania.types

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
        val BLACK = MoveColor(0,0,0)
        val RED = MoveColor(255,0,0)
        val ORANGE_ACTIVE = MoveColor(210,134,1)
        val ORANGE_INACTIVE = MoveColor(40,22,1)
        val ADMIN_BLUE_INACTIVE = MoveColor(0,0,63)
        val ADMIN_BLUE_ACTIVE = MoveColor(0,0,255)
    }

}

