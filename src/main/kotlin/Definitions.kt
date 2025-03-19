package de.vanfanel.joustmania


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