package de.vanfanel.joustmania

import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMovePairingManager
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.RainbowAnimation
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.firstOrNull

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/clear-devices") {
            PSMovePairingManager.disconnectAndForgetAllPairedPSMove()
        }

        get("/blink-red") {
            PSMoveApi.setAllMoveControllerToRed()
        }

        get("/setColor/{color}") {
            val color = call.parameters["color"]
            PSMoveApi.setColorOnAllMoveController(MoveColor.getColorByName(color?.uppercase()))
        }

        get("/setColorAnimation") {
            val moves = PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.firstOrNull()
            moves?.let {
                it.map { move -> move.setColorAnimation(RainbowAnimation) }
            }
        }
    }
}
