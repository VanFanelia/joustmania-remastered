package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.util.CustomThreadDispatcher
import de.vanfanel.joustmania.util.onlyAddedFromPrevious
import de.vanfanel.joustmania.util.onlyRemovedFromPrevious
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object GlobalMoveTicker {
    private val logger = KotlinLogging.logger {}

    private val currentMovesToWatch: MutableMap<MacAddress, PSMoveStub> = ConcurrentHashMap()

    private var calculateColorsJob: Job? = null // 50ms
    private var pollJobs: MutableMap<MacAddress, Job> = ConcurrentHashMap() // 5ms

    init {
        CoroutineScope(Dispatchers.IO).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.onlyRemovedFromPrevious().collect { moves ->
                moves.forEach { move ->
                    pollJobs[move.macAddress]?.cancel("move disconnected - cleanup old jobs")
                    pollJobs.remove(move.macAddress)
                    currentMovesToWatch.remove(move.macAddress)
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.onlyAddedFromPrevious().collect { moves ->
                for (move in moves) {
                    currentMovesToWatch.putIfAbsent(move.macAddress, move)
                    pollJobs[move.macAddress] = CoroutineScope(CustomThreadDispatcher.POLLING).launch {
                        while (true) {
                            val startOfPoll = System.nanoTime()
                            move.refreshMoveStatusAndEmitChanges()
                            val duration = (System.nanoTime() - startOfPoll) / 1000000
                            if (duration > 10) {
                                logger.warn { "PSMove status polling took $duration ms. This is more then the 10ms threshold." }
                            }
                            delay(10)
                        }
                    }
                }
            }
        }
        startColorCalculationJob()
    }

    fun startColorCalculationJob() {
        calculateColorsJob = CoroutineScope(CustomThreadDispatcher.COLOR_CALCULATION).launch {
            while (true) {
                logger.trace { "Starting color calculation job" }
                val startOfPoll = System.nanoTime()
                for (move in currentMovesToWatch) {
                    move.value.changeColorIfAnimationIsActive()
                }
                val duration = (System.nanoTime() - startOfPoll) / 1000000
                if (duration > 50) {
                    logger.debug { "PSMove color updates took $duration ms. This is more then the 50ms threshold." }
                    continue
                }
                delay(50)
            }
        }
    }

    fun stopPSMoveJobs() {
        pollJobs.values.forEach { it.cancel("stoped by method call") }
        calculateColorsJob?.cancel("stoped by method call")
    }
}