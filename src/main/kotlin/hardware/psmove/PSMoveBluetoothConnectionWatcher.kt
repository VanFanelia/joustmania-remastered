package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.PSMoveBatteryLevel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.thp.psmove.ConnectionType
import io.thp.psmove.PSMove
import io.thp.psmove.psmoveapi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap

/**
 * Object observes the amount of connected psmove controller and publishes the actual set of PSMoveController via flow
 *
 * Important! Duo to native Exceptions we do not publish PSMove classes outside this package.
 * We use the MACAddress as an identifier and if someone outside want to do stuff with a PSMove
 * PSMoveApi is the entry point / api to call
 */
object PSMoveBluetoothConnectionWatcher {
    private val logger = KotlinLogging.logger {}

    private const val PS_MOVE_BLUETOOTH_SCAN_INTERVAL = 2000L
    private var lastCountOfConnectedMoves = 0

    private val _bluetoothConnectedPSMoves: MutableStateFlow<Set<PSMoveStub>> = MutableStateFlow(emptySet())
    val bluetoothConnectedPSMoves: Flow<Set<PSMoveStub>> = _bluetoothConnectedPSMoves

    val connectedPSMoveController: Flow<Set<MacAddress>> =
        _bluetoothConnectedPSMoves.map { moveStubs -> moveStubs.map { it.macAddress }.toSet() }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allBatteryStates: Flow<Set<Pair<MacAddress, PSMoveBatteryLevel?>>> = _bluetoothConnectedPSMoves
        .flatMapLatest { moveStubs ->
            val flows = moveStubs.map { moveStub ->
                moveStub.batteryLevelFlow.map { batteryLevel ->
                    moveStub.macAddress to batteryLevel
                }
            }

            (if (flows.isEmpty()) {
                flowOf(emptySet())
            } else {
                combine(flows) { it.toSet() }
            })
        }


    private val _psmoveControllerMap: MutableMap<MacAddress, PSMove> = ConcurrentHashMap()

    suspend fun startEndlessLoopWithPSMoveConnectionScan() {
        val moveStubs = ConcurrentHashMap<MacAddress, PSMoveStub>()
        coroutineScope {
            while (true) {
                try {
                    val currentCount = psmoveapi.psmove_count_connected()
                    if (lastCountOfConnectedMoves == currentCount) {
                        delay(PS_MOVE_BLUETOOTH_SCAN_INTERVAL)
                        continue
                    }
                    lastCountOfConnectedMoves = currentCount

                    val moves = mutableMapOf<MacAddress, PSMove>()
                    for (i in 0..<currentCount) {
                        val move = PSMove(i)
                        if (move.connection_type == ConnectionType.Conn_USB.swigValue()) {
                            logger.info { "Ignore via usb connected ps move controller with mac: ${move._serial}" }
                            continue
                        }
                        moves[move.getMacAddress()] = move
                        moveStubs.putIfAbsent(move.getMacAddress(), PSMoveStub(move.getMacAddress()))

                        logger.info { "New move found: ${move.getMacAddress()}" }
                    }
                    for (movesToRemove: MacAddress in (moveStubs.keys - moves.keys)) {
                        moveStubs.remove(movesToRemove)
                    }
                    _bluetoothConnectedPSMoves.emit(moveStubs.values.toSet())
                    _psmoveControllerMap.clear()
                    _psmoveControllerMap.putAll(moves)
                } catch (e: Exception) {
                    logger.error(e) { "Error while watching for new connected PSMove Hardware via bluetooth" }
                }

                sleep(PS_MOVE_BLUETOOTH_SCAN_INTERVAL)
            }
        }
    }

    fun getMove(address: MacAddress): PSMove? {
        return _psmoveControllerMap[address]
    }

    fun getAllMoves(): Set<PSMove> {
        return _psmoveControllerMap.values.toSet()
    }
}

