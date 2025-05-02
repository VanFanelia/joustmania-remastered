package de.vanfanel.joustmania

import de.vanfanel.joustmania.hardware.AccelerationDebugger
import de.vanfanel.joustmania.hardware.BluetoothControllerManager
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMovePairingManager
import de.vanfanel.joustmania.hardware.USBDevicesChangeWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveLightRefresher
import de.vanfanel.joustmania.os.dependencies.NativeLoader
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


private val logger = KotlinLogging.logger {}

@Suppress("UNUSED_VARIABLE")
fun main() {
    logger.info { "start server" }

    // load native libraries
    @Suppress("unused")
    val nativeLoader = NativeLoader

    // init single Objects
    val usbDevicesChangeWatcher = USBDevicesChangeWatcher
    val psMoveBluetoothConnectionWatcher = PSMoveBluetoothConnectionWatcher

    @Suppress("unused")
    val bluetoothControllerManager = BluetoothControllerManager
    @Suppress("unused")
    val hardwareController = PSMovePairingManager
    @Suppress("unused")
    val gameStateManager = GameStateManager
    @Suppress("unused")
    val lightRefresher = PSMoveLightRefresher

    // TODO only activate acceleration debugger in debug mode?
    @Suppress("unused")
    val accelerationDebugger = AccelerationDebugger

    CoroutineScope(Dispatchers.IO).launch {
        usbDevicesChangeWatcher.startEndlessLoopWithUSBDevicesScan()
    }

    CoroutineScope(Dispatchers.IO).launch {
        psMoveBluetoothConnectionWatcher.startEndlessLoopWithPSMoveConnectionScan()
    }

    CoroutineScope(Dispatchers.IO).launch {
        psMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.collect { moves ->
            logger.info { "List of bluetooth connected PSMove Controller changed: " }
            logger.info { moves.map { move -> move.macAddress }}
        }
    }

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}


fun Application.module() {
    configureCORS()
    configureRouting()
}

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHost("localhost:5173", schemes = listOf("http"))
    }
}