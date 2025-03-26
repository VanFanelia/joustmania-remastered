package de.vanfanel.joustmania

import de.vanfanel.joustmania.hardware.psmove.PSMoveBroadcaster
import de.vanfanel.joustmania.hardware.psmove.PSMovePairingManager
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/clear-devices") {
            PSMovePairingManager.disconnectAndForgetAllPairedPSMove()
        }

        get("/blink-red") {
            PSMoveBroadcaster.setAllMoveControllerToRed()
        }
    }
}
