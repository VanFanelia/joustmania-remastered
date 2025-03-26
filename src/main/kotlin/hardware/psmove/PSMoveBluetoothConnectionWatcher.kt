package de.vanfanel.joustmania.hardware.psmove

import io.github.oshai.kotlinlogging.KotlinLogging
import io.thp.psmove.ConnectionType
import io.thp.psmove.PSMove
import io.thp.psmove.psmoveapi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.Thread.sleep

/**
 * Object observes the amount of connected psmove controller and publishes the actual set of PSMoveController via flow
 */
object PSMoveBluetoothConnectionWatcher {
    private val logger = KotlinLogging.logger {}

    private const val PS_MOVE_BLUETOOTH_SCAN_INTERVAL = 2000L
    private var lastCountOfConnectedMoves = 0

    private val _bluetoothConnectedPSMoves: MutableStateFlow<Set<PSMove>> = MutableStateFlow(emptySet())
    val bluetoothConnectedPSMoves: Flow<Set<PSMove>> = _bluetoothConnectedPSMoves

    suspend fun startEndlessLoopWithPSMoveConnectionScan() {
        coroutineScope {
            while (true) {

                try {
                    val currentCount = psmoveapi.psmove_count_connected()
                    if (lastCountOfConnectedMoves == currentCount) {
                        logger.debug { "no new devices found" }
                        delay(PS_MOVE_BLUETOOTH_SCAN_INTERVAL)
                        continue
                    }
                    lastCountOfConnectedMoves = currentCount
                    val moves = mutableSetOf<PSMove>()
                    for (i in 0..< currentCount) {
                        val move = PSMove(i)
                        if (move.connection_type == ConnectionType.Conn_USB.swigValue()) {
                            logger.info { "Ignore via usb connected ps move controller with mac: ${move._serial}" }
                            continue
                        }
                        moves.add(move)

                        logger.info { "New move found: ${move.getMacAddress()}" }
                    }
                    _bluetoothConnectedPSMoves.emit(moves)
                } catch (e: Exception) {
                    logger.error(e) { "Error while watching for new connected PSMove Hardware via bluetooth" }
                }

                sleep(PS_MOVE_BLUETOOTH_SCAN_INTERVAL)
            }
        }
    }

    fun getCurrentConnectedPSMove(): Set<PSMove> {
        return _bluetoothConnectedPSMoves.value
    }
}

