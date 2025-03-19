package de.vanfanel.joustmania

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "start server" }

    // load native libraries
    val nativeLoader = NativeLoader

    // init single Objects
    val usbDevicesChangeWatcher = USBDevicesChangeWatcher
    val bluetoothControllerManager = BlueToothControllerManager
    val hardwareController = PSMoveControllerManager

    CoroutineScope(Dispatchers.IO).launch {
        usbDevicesChangeWatcher.startEndlessLoopWithUSBDevicesScan()
    }

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}


fun Application.module() {
    configureRouting()
}
