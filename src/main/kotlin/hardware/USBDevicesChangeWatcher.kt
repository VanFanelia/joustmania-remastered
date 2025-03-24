package de.vanfanel.joustmania.hardware

import de.vanfanel.joustmania.types.BluetoothInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Thread.sleep

/**
 * Object observe usb ports via lsusb command
 */
object USBDevicesChangeWatcher {

    private val logger = KotlinLogging.logger {}
    private val lastFoundDevices = mutableSetOf<BluetoothInfo>()

    private val _usbDevicesChangeFlow: MutableStateFlow<Set<BluetoothInfo>> = MutableStateFlow(emptySet())
    val usbDevicesChangeFlow: Flow<Set<BluetoothInfo>> = _usbDevicesChangeFlow

    suspend fun startEndlessLoopWithUSBDevicesScan() {
        coroutineScope {
            while (true) {
                try {
                    logger.debug { "Try to find new connected usb devices" }
                    val process =
                        ProcessBuilder(listOf("lsusb")).start()

                    val currentDevices = mutableSetOf<BluetoothInfo>()

                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val trimmedLine: String = line!!.toString().trim()
                            try {
                                // parse this
                                // Bus 007 Device 001: ID 1d6b:0003 Linux Foundation 3.0 root hub
                                val bus = trimmedLine.substring(4, 7)
                                val device = trimmedLine.substring(15,18)
                                val id = trimmedLine.substring(23,32)
                                val name = trimmedLine.substring(33)
                                currentDevices.add(BluetoothInfo(bus = bus, device = device, id = id, nameWithType = name.trim()))
                            } catch (e: StringIndexOutOfBoundsException) {
                                logger.error(e) { "Failed to parse lsusb output" }
                                continue
                            }
                        }
                    }
                    process.waitFor()

                    logger.debug { "Old USB Devices list #${lastFoundDevices.size}, new USB list #${currentDevices.size}" }

                    if (lastFoundDevices != currentDevices) {
                        lastFoundDevices.clear()
                        lastFoundDevices.addAll(currentDevices)
                        _usbDevicesChangeFlow.emit(currentDevices)
                    }

                } catch (e: Exception) {
                    logger.error(e) { "Error while scanning for new usb devices" }
                }

                sleep(1000)
            }
        }
    }
}

