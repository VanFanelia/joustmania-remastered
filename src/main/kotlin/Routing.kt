package de.vanfanel.joustmania

import de.vanfanel.joustmania.games.GetSensitivity
import de.vanfanel.joustmania.games.Sensibility
import de.vanfanel.joustmania.games.Sensibility.Companion.parseSensibility
import de.vanfanel.joustmania.games.SetSensitivity
import de.vanfanel.joustmania.games.Settings
import de.vanfanel.joustmania.hardware.AccelerationDebugger
import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMovePairingManager
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.RainbowAnimation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.firstOrNull


private val logger = KotlinLogging.logger {}
fun Application.configureRouting() {
    // add base path// add base path
    routing {

        staticResources("/", "static", "index.html")

        route("/api") {

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
                SoundManager.asyncAddSoundToQueue(id = SoundId.GAME_MODE_FFA_EXPLANATION)
                logger.info { "explanation played" }
                call.respond(HttpStatusCode.Accepted)
            }

            get("/rumble/{intensity}") {
                logger.info { "Rumble all move controller" }
                val intensity = call.parameters["intensity"]?.toIntOrNull() ?: 0
                PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.firstOrNull()?.map { move ->
                    PSMoveApi.rumble(macAddress = move.macAddress, intensity = intensity)
                }
                call.respond(HttpStatusCode.NoContent)
            }

            get("/accelerations") {
                val json = AccelerationDebugger.getHistoryAsJson()
                call.respondText(json, contentType = ContentType.Application.Json)
            }

            get("/settings/sensitivity") {
                call.respond(status = HttpStatusCode.OK, message = GetSensitivity(Settings.getSensibility().name))
            }

            post("/settings/sensitivity") {
                val newSensitivity = call.receive<SetSensitivity>()
                val sensibility: Sensibility =
                    parseSensibility(newSensitivity.sensitivity) ?: return@post call.respond(HttpStatusCode.BadRequest)
                Settings.setSensibility(sensibility)
                call.respond(HttpStatusCode.OK, "Sensitivity updated to $sensibility")
            }
        }
    }
}
