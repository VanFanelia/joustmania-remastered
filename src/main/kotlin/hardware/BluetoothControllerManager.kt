package de.vanfanel.joustmania.hardware

import de.vanfanel.joustmania.types.AdapterId
import de.vanfanel.joustmania.types.BlueToothController
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.PairedDevice
import de.vanfanel.joustmania.hardware.USBDevicesChangeWatcher.usbDevicesChangeFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.exceptions.DBusException
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.ObjectManager
import org.freedesktop.dbus.types.Variant


// Magic adapter interface
@DBusInterfaceName("org.bluez.Adapter1")
interface Adapter1 : DBusInterface {
    fun RemoveDevice(device: DBusPath)
}

// Magic Property Set interface for trusting devices
@DBusInterfaceName("org.freedesktop.DBus.Properties")
interface DBusProperties : DBusInterface {
    fun Set(interfaceName: String, propertyName: String, value: Variant<*>)
}

/**
 * Object observes the devices connected via usb and publish
 * all connected bluetooth adapters and there paired devices
 */
object BluetoothControllerManager {
    private val logger = KotlinLogging.logger {}
    private val blueToothController = mutableMapOf<AdapterId, BlueToothController>()

    private val _pairedDevices: MutableStateFlow<Set<PairedDevice>> = MutableStateFlow(emptySet())
    val pairedDevices: Flow<Set<PairedDevice>> = _pairedDevices

    init {
        CoroutineScope(Dispatchers.IO).launch {
            usbDevicesChangeFlow.collect { _ ->
                detectBluetoothController()
            }
        }
    }

    private fun getAllManagedObjects(): Map<DBusPath, Map<String, Map<String, Variant<*>>>> {
        val connection = DBusConnectionBuilder.forSystemBus().build()
        try {
            val objectManager = connection.getRemoteObject(
                "org.bluez",
                "/",
                ObjectManager::class.java
            )

            val managedObjects: Map<DBusPath, Map<String, Map<String, Variant<*>>>> = objectManager.GetManagedObjects()

            return managedObjects
        } finally {
            connection.disconnect()
        }
    }

    private fun getAllManagedAdapterKeys() = getAllManagedObjects().keys.map { it.path }

    fun clearBluetoothDeviceFromAdapter(macAddress: MacAddress) {
        val managedObjectKeys = getAllManagedObjects().keys.map { it.path }

        for (adapterId in blueToothController.keys) {
            val connection = DBusConnectionBuilder.forSystemBus().build()
            try {
                val adapterPath = getAdapterPath(adapterId = adapterId)
                val devicePath = getDevicePath(adapterId = adapterId, macAddress = macAddress)

                if (!managedObjectKeys.contains(devicePath)) {
                    logger.info { "Adapter $adapterId has no device with macAddress $macAddress . Nothing to do." }
                    continue
                }

                logger.info { "Try to removing device with path: $devicePath" }
                val adapterObject =
                    connection.getRemoteObject("org.bluez", adapterPath, Adapter1::class.java) as Adapter1
                adapterObject.RemoveDevice(DBusPath(devicePath))

                CoroutineScope(Dispatchers.IO).launch {
                    removeFromPairedDeviceList(macAddress)
                }

                logger.info { "Device $macAddress removed successfully from adapter: $adapterId" }
            } catch (e: DBusException) {
                logger.error(e) { "Failed to remove device with Mac: $macAddress from Adapter with id: $adapterId" }
            } finally {
                connection.disconnect()
            }
        }
    }

    fun trustBluetoothDevice(macAddress: String) {
        for (adapterId in blueToothController.keys) {
            val connection = DBusConnectionBuilder.forSystemBus().build()
            try {
                val devicePath = getDevicePath(adapterId = adapterId, macAddress = macAddress)
                logger.info { "Try to trust device with path: $devicePath" }

                if (!getAllManagedAdapterKeys().contains(devicePath)) {
                    logger.info { "Adapter $adapterId has no device with macAddress $macAddress . Nothing to do." }
                    continue
                }

                logger.info { "Trying to trust device at path: $devicePath" }

                val properties = connection.getRemoteObject(
                    "org.bluez",
                    devicePath,
                    DBusProperties::class.java
                )

                // set trusted flag to true
                properties.Set("org.bluez.Device1", "Trusted", Variant(true))
                logger.info { "Device $macAddress is now trusted" }
            } catch (e: DBusException) {
                logger.error(e) { "Failed to trust device with Mac: $macAddress from Adapter with id: $adapterId" }
            } finally {
                connection.disconnect()
            }
        }
    }

    private suspend fun detectBluetoothController() {
        logger.info { "try to detect bluetooth adapters..." }
        blueToothController.clear()
        try {
            val connection = DBusConnectionBuilder.forSystemBus().build()

            val objectManager = connection.getRemoteObject(
                "org.bluez",
                "/",
                ObjectManager::class.java
            )

            val managedObjects: Map<DBusPath, Map<String, Map<String, Variant<*>>>> =
                objectManager.GetManagedObjects()

            for ((objectPath, interfaces) in managedObjects) {
                // filter adapters like (hci0, hci1, ...)
                if ("org.bluez.Adapter1" in interfaces) {
                    val adapterId =
                        objectPath.path?.replace(oldValue = "/org/bluez/", newValue = "", ignoreCase = true)
                            ?: continue
                    val macAddress =
                        interfaces["org.bluez.Adapter1"]?.get("Address")?.value?.toString()?.replace("[", "")
                            ?.replace("]", "") ?: continue
                    val pairedDevices = filterPairedBluetoothDevices(
                        adapterId = adapterId,
                        adapterObjectPath = objectPath.path,
                        managedObjects = managedObjects
                    )

                    val controller = BlueToothController(
                        adapterId = adapterId,
                        macAddress = macAddress,
                        pairedDevices = pairedDevices,
                        pairedMotionController = pairedDevices.filter {
                            it.name.contains(
                                PLAYSTATION_MOTION_CONTROLLER_USB_SHORT_DEVICE_NAME
                            )
                        }.toSet()
                    )
                    logger.info { "Found Adapter: $controller" }
                    blueToothController[adapterId] = controller
                }
            }

            logger.debug { "Found ${blueToothController.size} Adapters" }
            val connectedMotionControllersAddresses = getAllPairedMotionControllers().map { it.macAddress }
            logger.debug { "Found ${connectedMotionControllersAddresses.size} paired motion controllers: $connectedMotionControllersAddresses" }
            connection.disconnect()
            _pairedDevices.emit(blueToothController.values.flatMap { it.pairedDevices }.toSet())
        } catch (e: DBusException) {
            e.printStackTrace()
        }
    }

    private fun filterPairedBluetoothDevices(
        adapterId: AdapterId,
        adapterObjectPath: String,
        managedObjects: Map<DBusPath, Map<String, Map<String, Variant<*>>>>
    ): Set<PairedDevice> {
        return managedObjects.entries.asSequence().filter { entry ->
            entry.key.path.startsWith(
                prefix = adapterObjectPath,
                ignoreCase = true
            )
        }.filter { entry -> entry.value.containsKey("org.bluez.Device1") }
            .mapNotNull { entry -> entry.value["org.bluez.Device1"] }
            .filter { entry -> entry.containsKey("Address") && entry.containsKey("Name") }
            .map { entry ->
                PairedDevice(
                    adapterId = adapterId,
                    macAddress = (entry["Address"]?.toString()?.replace("[", "")
                        ?.replace("]", "") ?: ""),
                    name = (entry["Name"]?.toString() ?: ""),
                    paired = entry["Paired"]?.value as Boolean?,
                    connected = entry["Connected"]?.value as Boolean?
                )
            }
            .toSet()
    }

    private suspend fun removeFromPairedDeviceList(macAddress: MacAddress) {
        val current = _pairedDevices.value
        _pairedDevices.emit(current.filter { it.macAddress != macAddress }.toSet())
    }

    fun getAdapterForPairing(): BlueToothController? {
        return blueToothController.minWithOrNull(compareBy { it.value.pairedMotionController.size })?.value
    }

    private fun getAllPairedMotionControllers(): Set<PairedDevice> {
        return blueToothController.values.map { it.pairedMotionController }.flatten().toSet()
    }
}

private fun getAdapterPath(adapterId: AdapterId): String {
    return "/org/bluez/$adapterId"
}

private fun getDevicePath(adapterId: AdapterId, macAddress: MacAddress): String {
    val adapterPath = getAdapterPath(adapterId = adapterId)
    return adapterPath + "/dev_" + macAddress.replace(":", "_")
}

