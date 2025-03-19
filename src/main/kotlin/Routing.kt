package de.vanfanel.joustmania

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/clear-devices") {
            PSMoveControllerManager.disconnectAndForgetAllPairedPSMove()
        }

        get("/blink-red") {
            PSMoveControllerManager.blinkRed()
        }
    }
}
