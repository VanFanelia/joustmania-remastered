package de.vanfanel.joustmania.hardware


import de.vanfanel.joustmania.hardware.bluetooth.BluetoothCommands.restartBluetooth
import de.vanfanel.joustmania.hardware.bluetooth.BluetoothControllerManager.pairedDevices
import de.vanfanel.joustmania.hardware.usb.USBDevicesChangeWatcher.usbDevicesChangeFlow
import de.vanfanel.joustmania.hardware.bluetooth.BluetoothControllerManager
import de.vanfanel.joustmania.hardware.psmove.getMacAddress
import de.vanfanel.joustmania.hardware.psmove.indicatePairingComplete
import de.vanfanel.joustmania.hardware.psmove.trust
import de.vanfanel.joustmania.types.PairedDevice
import de.vanfanel.joustmania.util.CustomThreadDispatcher
import io.github.oshai.kotlinlogging.KotlinLogging
import io.thp.psmove.ConnectionType
import io.thp.psmove.PSMove
import io.thp.psmove.psmoveapi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val PLAYSTATION_MOTION_CONTROLLER_USB_DEVICE_NAME = "PlayStation Move motion controller"
const val PLAYSTATION_MOTION_CONTROLLER_USB_SHORT_DEVICE_NAME = "Motion Controller"

/**
 * Object observe via usb connected devices and auto pair usb connected Move controller
 */
object PSMovePairingManager {

    private val logger = KotlinLogging.logger {}
    private val pairedMoveController = mutableSetOf<PairedDevice>()

    init {
        CoroutineScope(CustomThreadDispatcher.BLUETOOTH).launch {
            pairedDevices.collect { pairedDevices ->
                pairedDevices.map { device ->
                    checkForMoveControllerAndAddToListIfMatched(device)
                }
            }
        }

        CoroutineScope(CustomThreadDispatcher.BLUETOOTH).launch {
            usbDevicesChangeFlow.collect { allConnectedUSBDevices ->
                logger.debug { "new usb devices connected. Current connected devices: $allConnectedUSBDevices" }
                allConnectedUSBDevices.map { device ->
                    if (device.nameWithType.contains(PLAYSTATION_MOTION_CONTROLLER_USB_DEVICE_NAME)) {
                        motionControllerViaUSBConnected()
                    }
                }
            }
        }
    }

    private fun motionControllerViaUSBConnected() {
        logger.info { "try to connect new psmove controller" }
        val currentCount = psmoveapi.psmove_count_connected()
        logger.info { "Found $currentCount motion controller" }

        for (i in 0..<currentCount) {
            val move = PSMove(i)
            logger.info { "New move found: ${move.getMacAddress()}" }

            if (move.connection_type == ConnectionType.Conn_USB.swigValue() && !pairedMoveController.map { it.macAddress.uppercase() }
                    .contains(move.getMacAddress())) {
                val adapter = BluetoothControllerManager.getAdapterForPairing()

                logger.info { "Try to pair new device: ${move.getMacAddress()} to ${adapter?.macAddress ?: "unknown"}" }
                if (adapter?.macAddress == null) {
                    logger.error { "Cannot pair motion controller ${move.getMacAddress()} because no adapter to pair found" }
                    continue
                }

                CoroutineScope(CustomThreadDispatcher.BLUETOOTH).launch {
                    val pairedResult = move.pair_custom(adapter.macAddress)
                    logger.info { "Pairing returned: $pairedResult" }

                    //move.connect(adapter.adapterId)
                    move.trust()
                    move.indicatePairingComplete()
                    restartBluetooth()
                }
            } else {
                logger.info { "Move with address ${move.getMacAddress()} is already paired or not connected via usb" }
            }

        }
    }

    private fun checkForMoveControllerAndAddToListIfMatched(device: PairedDevice) {
        if (device.name.contains(PLAYSTATION_MOTION_CONTROLLER_USB_SHORT_DEVICE_NAME)) {
            logger.info { "Found Paired Move Controller: $device" }
            if (pairedMoveController.add(device)) {
                logger.info { "Found new paired move controller: $device" }
            }
        }
    }

    fun disconnectAndForgetAllPairedPSMove() {
        CoroutineScope(CustomThreadDispatcher.BLUETOOTH).launch {
            pairedMoveController.map {
                logger.info { "Try to disconnect and forget Move Controller with Mac: ${it.macAddress}" }
                BluetoothControllerManager.clearBluetoothDeviceFromAdapter(it.macAddress)
            }
            pairedMoveController.clear()
            restartBluetooth()
        }

    }

}
