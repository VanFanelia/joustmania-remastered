package de.vanfanel.joustmania

import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMovePairingManager
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.RainbowAnimation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.firstOrNull


private val logger = KotlinLogging.logger {}
fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/clear-devices") {
            PSMovePairingManager.disconnectAndForgetAllPairedPSMove()
            call.respond(HttpStatusCode.NoContent)
        }

        get("/setColor/{color}") {
            val color = call.parameters["color"]
            PSMoveApi.setColorOnAllMoveController(MoveColor.getColorByName(color?.uppercase()))
            call.respond(HttpStatusCode.NoContent)
        }

        get("/setColorAnimation") {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.firstOrNull()?.map { move ->
                move.setColorAnimation(RainbowAnimation)
            }
            call.respond(HttpStatusCode.NoContent)
        }

        get("/setRedAnimationTest") {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.firstOrNull()?.map { move ->
                move.setColorAnimation(
                    ColorAnimation(
                        colorToSet = listOf(
                            MoveColor.RED,
                            MoveColor.RED_INACTIVE
                        ), durationInMS = 1000,
                        loop = false
                    )
                )
            }
            call.respond(HttpStatusCode.NoContent)
        }

        get("/playsoundtest") {
            logger.info { "play explanation..." }
            SoundManager.asyncAddSoundToQueue(SoundId.GAME_MODE_FFA_EXPLANATION)
            logger.info { "explanation played" }
            call.respond(HttpStatusCode.Accepted)
        }

        get("/rumble/{intensity}") {
            logger.info { "Rumble all move controller" }
            val intensity = call.parameters["intensity"]?.toIntOrNull() ?: 0
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.firstOrNull()?.map { move ->
                PSMoveApi.rumble(macAddress = move.macAddress, intensity = intensity )
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
