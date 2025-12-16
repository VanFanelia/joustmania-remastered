package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.util.CustomThreadDispatcher
import de.vanfanel.joustmania.util.NamedThreadFactory
import de.vanfanel.joustmania.util.onlyAddedFromPrevious
import de.vanfanel.joustmania.util.onlyRemovedFromPrevious
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * The `GlobalMoveTicker` singleton is responsible for managing and monitoring the connected PSMove controllers.
 * It performs periodic tasks such as polling the status of connected devices and calculating color changes for active animations.
 *
 * This class uses coroutines extensively to perform tasks asynchronously and efficiently.
 * It relies on multiple custom dispatchers for different operations, such as polling and color calculations.
 *
 * Key responsibilities:
 * - Performs frequent polling the status of active PSMove devices to ensure up-to-date state information.
 * - Initiates periodic color calculation tasks to handle animations on connected devices.
 */
object GlobalMoveTicker {
    private val logger = KotlinLogging.logger {}

    private val currentMovesToWatch: MutableMap<MacAddress, PSMoveStub> = ConcurrentHashMap()

    // Each controller gets its own dedicated thread to prevent blocking issues
    private val controllerDispatchers: MutableMap<MacAddress, ExecutorCoroutineDispatcher> = ConcurrentHashMap()
    private var pollJobs: MutableMap<MacAddress, Job> = ConcurrentHashMap()

    private var pollingThread: Thread? = null

    @Volatile
    private var shouldStopPolling = false

    init {
        CoroutineScope(CustomThreadDispatcher.BLUETOOTH).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.onlyRemovedFromPrevious().collect { moves ->
                moves.forEach { move ->
                    currentMovesToWatch.remove(move.macAddress)
                    // Clean up dispatcher for removed controller
                    controllerDispatchers.remove(move.macAddress)?.close()
                    logger.debug { "Removed dispatcher for controller ${move.macAddress}" }
                }
            }
        }

        CoroutineScope(CustomThreadDispatcher.BLUETOOTH).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.onlyAddedFromPrevious().collect { moves ->
                for (move in moves) {
                    currentMovesToWatch.putIfAbsent(move.macAddress, move)
                    // Create a dedicated dispatcher for this controller
                    if (!controllerDispatchers.containsKey(move.macAddress)) {
                        val dispatcher = Executors.newSingleThreadExecutor(
                            NamedThreadFactory("PSMove-${move.macAddress.takeLast(8)}", priority = 6)
                        ).asCoroutineDispatcher()
                        controllerDispatchers[move.macAddress] = dispatcher
                        logger.debug { "Created dedicated dispatcher for controller ${move.macAddress}" }
                    }
                }
            }
        }

        // start the most important Thread in this hole app
        val pollingThread = Thread {
            while (true) {
                val startOfPoll = System.nanoTime()
                if (shouldStopPolling) break

                val unfinishedMoves: MutableList<MacAddress> = mutableListOf()
                for (entry in pollJobs.entries) {
                    if (!entry.value.isCompleted) {
                        unfinishedMoves.add(entry.key)
                    }
                }

                for (move in currentMovesToWatch.values) {
                    if (move.macAddress in unfinishedMoves) {
                        logger.debug { "Move ${move.macAddress} is still polling. Skip it :(" }
                        continue
                    }
                    // Use dedicated dispatcher for this controller to prevent blocking others
                    val dispatcher = controllerDispatchers[move.macAddress]
                    if (dispatcher == null) {
                        logger.warn { "No dispatcher found for controller ${move.macAddress}" }
                        continue
                    }
                    val job = CoroutineScope(dispatcher + SupervisorJob()).launch {
                        move.refreshMoveStatusAndEmitChanges()
                        calculateColorForMove(move.macAddress)
                    }
                    pollJobs[move.macAddress] = job
                }

                val durationInMs = (System.nanoTime() - startOfPoll) / 1000000
                if (durationInMs > 5) {
                    logger.trace { "PSMove status polling took $durationInMs ms. This is more then the 5ms threshold." }
                }
                Thread.sleep(5)
            }
        }
        pollingThread.name = "PSMove polling thread (max Prio)"
        pollingThread.priority = Thread.MAX_PRIORITY
        pollingThread.isDaemon = false
        pollingThread.start()
    }

    private fun calculateColorForMove(moveId: MacAddress) {
        logger.trace { "Starting color calculation job for $moveId" }
        val startOfPoll = System.nanoTime()
        currentMovesToWatch[moveId]?.changeColorIfAnimationIsActive()
        val duration = (System.nanoTime() - startOfPoll) / 1000000
        if (duration > 10) {
            logger.debug { "PSMove color updates took $duration ms. This is more then the 10ms threshold." }
        }
    }

    fun stopPSMoveJobs() {
        pollingThread?.interrupt() // Thread unterbrechen
        pollingThread?.join(1000) // Warten bis Thread beendet ist (max 1s)

        // Close all controller-specific dispatchers
        controllerDispatchers.values.forEach { dispatcher ->
            dispatcher.close()
        }
        controllerDispatchers.clear()
        logger.info { "Closed all controller dispatchers" }
    }
}