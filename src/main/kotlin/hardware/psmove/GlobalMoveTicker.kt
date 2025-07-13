package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.util.SingleThreadDispatcher
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object GlobalMoveTicker {
    private val logger = KotlinLogging.logger {}

    private val lock = ReentrantLock()
    private val currentMovesToWatch: MutableMap<MacAddress, PSMoveStub> = ConcurrentHashMap()

    private var colorJob: kotlinx.coroutines.Job? = null // 50ms
    private var pollJob: kotlinx.coroutines.Job? = null // 5ms

    init {
        CoroutineScope(Dispatchers.IO).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.collect { moveStubs ->
                lock.withLock {
                    currentMovesToWatch.clear()
                    currentMovesToWatch.putAll(moveStubs.associateBy { it.macAddress })
                }

            }
        }

        startPSMoveJobs()
    }

    fun startPSMoveJobs() {
        pollJob = CoroutineScope(SingleThreadDispatcher.BUTTONS).launch {
            while (true) {
                val startOfPoll = System.nanoTime()
                lock.withLock {
                    for (move in currentMovesToWatch) {
                        move.value.pollMoveControllerState()
                    }
                }
                val duration = (System.nanoTime() - startOfPoll) / 1000000
                if (duration > 5) {
                    logger.debug { "PSMove status polling took $duration ms. This is more then the 5ms threshold." }
                    continue
                }
                delay(5 - duration)
            }
        }

        colorJob = CoroutineScope(SingleThreadDispatcher.COLORS).launch {
            while (true) {
                val startOfPoll = System.nanoTime()
                lock.withLock {
                    for (move in currentMovesToWatch) {
                        move.value.changeColor()
                    }
                }
                val duration = (System.nanoTime() - startOfPoll) / 1000000
                if (duration > 50) {
                    logger.debug { "PSMove color updates took $duration ms. This is more then the 50ms threshold." }
                    continue
                }
                delay(50 - duration)
            }
        }
    }

    fun stopPSMoveJobs() {
        pollJob?.cancel("stoped by method call")
        colorJob?.cancel("stoped by method call")
    }
}