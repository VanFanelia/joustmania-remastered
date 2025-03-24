package de.vanfanel.joustmania

import de.vanfanel.joustmania.game.GameStateManager
import de.vanfanel.joustmania.hardware.BluetoothControllerManager
import de.vanfanel.joustmania.hardware.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.PSMoveButtonObserver
import de.vanfanel.joustmania.hardware.PSMovePairingManager
import de.vanfanel.joustmania.hardware.USBDevicesChangeWatcher
import de.vanfanel.joustmania.os.dependencies.NativeLoader
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
    val psMoveBluetoothConnectionWatcher = PSMoveBluetoothConnectionWatcher
    val bluetoothControllerManager = BluetoothControllerManager
    val hardwareController = PSMovePairingManager
    val gameStateManager = GameStateManager
    val psMoveButtonObserver = PSMoveButtonObserver

    psMoveButtonObserver.startButtonObserver()

    CoroutineScope(Dispatchers.IO).launch {
        usbDevicesChangeWatcher.startEndlessLoopWithUSBDevicesScan()
    }

    CoroutineScope(Dispatchers.IO).launch {
        psMoveBluetoothConnectionWatcher.startEndlessLoopWithPSMoveConnectionScan()
    }

    CoroutineScope(Dispatchers.IO).launch {
        psMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.collect { moves ->
            logger.info { "List of bluetooth connected PSMove Controller changed: " }
            logger.info { moves.map { move -> move._serial }}
        }
    }

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}


fun Application.module() {
    configureRouting()
}
