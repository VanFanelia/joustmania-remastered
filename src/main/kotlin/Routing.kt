package de.vanfanel.joustmania

import de.vanfanel.joustmania.GameStateManager.currentGameState
import de.vanfanel.joustmania.games.Language
import de.vanfanel.joustmania.games.Language.Companion.parseLanguage
import de.vanfanel.joustmania.games.Sensibility
import de.vanfanel.joustmania.games.Sensibility.Companion.parseSensibility
import de.vanfanel.joustmania.games.SetLanguage
import de.vanfanel.joustmania.games.SetSensitivity
import de.vanfanel.joustmania.games.Settings
import de.vanfanel.joustmania.hardware.AccelerationDebugger
import de.vanfanel.joustmania.hardware.BluetoothControllerManager.blueToothControllerFlow
import de.vanfanel.joustmania.hardware.psmove.ColorAnimation
import de.vanfanel.joustmania.hardware.psmove.PSMoveApi
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher.allBatteryStates
import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher.connectedPSMoveController
import de.vanfanel.joustmania.hardware.psmove.PSMovePairingManager
import de.vanfanel.joustmania.lobby.LobbyLoop
import de.vanfanel.joustmania.lobby.LobbyLoop.activeMoves
import de.vanfanel.joustmania.lobby.LobbyLoop.controllersWithAdminRights
import de.vanfanel.joustmania.sound.SoundId
import de.vanfanel.joustmania.sound.SoundManager
import de.vanfanel.joustmania.types.BlueToothControllerStats
import de.vanfanel.joustmania.types.GameStats
import de.vanfanel.joustmania.types.MotionControllerStats
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.RainbowAnimation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json


private val logger = KotlinLogging.logger {}
fun Application.configureRouting() {
    routing {

        // TODO enable me
        // staticResources("/", "static", "index.html")

        route("/api") {

            // Debug routes
            get("/") {
                call.respondText("Hello World!")
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

            get("/rumble/{intensity}") {
                logger.info { "Rumble all move controller" }
                val intensity = call.parameters["intensity"]?.toIntOrNull() ?: 0
                PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.firstOrNull()?.map { move ->
                    PSMoveApi.rumble(macAddress = move.macAddress, intensity = intensity)
                }
                call.respond(HttpStatusCode.NoContent)
            }

            get("/playsoundtest") {
                logger.info { "play explanation..." }
                SoundManager.asyncAddSoundToQueue(id = SoundId.GAME_MODE_FFA_EXPLANATION)
                logger.info { "explanation played" }
                call.respond(HttpStatusCode.Accepted)
            }

            get("/playsound/{soundId}") {
                logger.info { "play sound..." }
                val soundName: String? = call.parameters["soundId"]
                val soundId = SoundId.fromString(soundName)
                if (soundId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Cannot find Sound Id")
                    return@get
                }

                SoundManager.asyncAddSoundToQueue(id = soundId, abortOnNewSound = false)
                logger.info { "play sound id: $soundId" }
                call.respond(HttpStatusCode.Accepted)
            }

            // Hardware commands
            get("/clear-devices") {
                PSMovePairingManager.disconnectAndForgetAllPairedPSMove()
                call.respond(HttpStatusCode.NoContent)
            }

            get("/accelerations") {
                val json = AccelerationDebugger.getHistoryAsJson()
                call.respondText(json, contentType = ContentType.Application.Json)
            }

            // change settings

            post("/settings/sensitivity") {
                val newSensitivity = call.receive<SetSensitivity>()
                val sensibility: Sensibility =
                    parseSensibility(newSensitivity.sensitivity) ?: return@post call.respond(HttpStatusCode.BadRequest)
                Settings.setSensibility(sensibility)
                call.respond(HttpStatusCode.OK, "Sensitivity updated to $sensibility")
            }

            post("/settings/language") {
                val newLanguage = call.receive<SetLanguage>()
                val language: Language =
                    parseLanguage(newLanguage.language) ?: return@post call.respond(HttpStatusCode.BadRequest)
                Settings.setLanguage(language)
                call.respond(HttpStatusCode.OK, "Language updated to $language")
            }

            // manipulate game
            post("/game/force-start") {
                val gameState = currentGameState.firstOrNull()
                if(gameState == GameState.LOBBY) {
                    val activeMoves = activeMoves.firstOrNull()
                    if (activeMoves.isNullOrEmpty()) {
                        logger.warn { "force start was called but no active moves found. Request ignored." }
                        call.respond(HttpStatusCode.BadRequest, "No active moves found")
                        return@post
                    }

                    logger.info { "force start was called. Start current selected game now." }
                    LobbyLoop.startGame()
                    call.respond(HttpStatusCode.OK, "Game started")
                    return@post
                }
                logger.warn { "force start was called but game state was not in 'GameState.LOBBY'. Request ignored." }
                call.respond(HttpStatusCode.BadRequest, "Force start can only be called during lobby state")
            }

            post("/game/force-stop") {
                val gameState = currentGameState.firstOrNull()
                if(gameState == GameState.GAME_RUNNING) {
                    GameStateManager.forceStopGame()
                    logger.info { "force stop was called. Stop game now!" }
                    call.respond(HttpStatusCode.OK, "Game stoped")
                    return@post
                }
                logger.warn { "force stop was called but game state was not in 'GameState.RUNNING'. Request ignored." }
                call.respond(HttpStatusCode.BadRequest, "Cannot force stop game if game state is: $gameState. Only 'Running' games can be stoped'")
            }

            // event streams
            get("/sse/settings") {
                call.response.cacheControl(CacheControl.NoCache(null))
                logger.debug { "new client connected to sse/settings endpoint" }
                call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                    Settings.currentConfigFlow.collect { value ->
                        logger.debug { "new settings pushed to client: $value" }
                        write("data: ${Json.encodeToString(value)}\n\n")
                        flush()
                    }
                }
            }

            get("/sse/bluetooth") {
                call.response.cacheControl(CacheControl.NoCache(null))
                logger.debug { "new client connected to sse/bluetooth endpoint" }
                call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                    val combinedFlow: Flow<Set<BlueToothControllerStats>> = combine(
                        blueToothControllerFlow,
                        controllersWithAdminRights,
                        allBatteryStates,
                        connectedPSMoveController
                    ) { controllers, admins, batteryStates, connected ->
                        return@combine controllers.map { blueToothController ->
                            BlueToothControllerStats(
                                adapterId = blueToothController.adapterId,
                                macAddress = blueToothController.macAddress,
                                name = blueToothController.name,
                                pairedMotionController = blueToothController.pairedMotionController.map { motionController ->
                                    MotionControllerStats(
                                        adapterId = blueToothController.adapterId,
                                        macAddress = motionController.macAddress,
                                        connected = connected.contains(motionController.macAddress),
                                        isAdmin = admins.contains(motionController.macAddress),
                                        batteryLevel = batteryStates.find { it.first == motionController.macAddress }?.second?.value
                                    )
                                }.toSet()
                            )
                        }.toSet()
                    }
                    combinedFlow.collect { controllers ->
                        logger.debug { "new bluetooth controllers list pushed to client: $controllers" }
                        write("data: ${Json.encodeToString(controllers)}\n\n")
                        flush()
                    }
                }
            }

            get("/sse/game") {
                call.response.cacheControl(CacheControl.NoCache(null))
                logger.debug { "new client connected to sse/game endpoint" }
                call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                    val combinedFlow: Flow<GameStats> = combine(
                        currentGameState,
                        activeMoves,
                    ) { gameState, activeMoveList ->
                        return@combine GameStats(
                            currentGameState = gameState.toDisplayString(),
                            activeController = activeMoveList
                        )
                    }
                    combinedFlow.collect { gameStats ->
                        logger.debug { "new gameStats pushed to client: $gameStats" }
                        write("data: ${Json.encodeToString(gameStats)}\n\n")
                        flush()
                    }
                }
            }
        }
    }
}
