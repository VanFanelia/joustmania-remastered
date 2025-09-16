package de.vanfanel.joustmania.hardware

import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.util.CustomThreadDispatcher
import de.vanfanel.joustmania.util.FixedSizeQueue
import de.vanfanel.joustmania.util.onlyRemovedFromPrevious
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class Acceleration(
    val t: Long,
    val a: Double
)

@Serializable
data class StubAcceleration(
    val mac: MacAddress,
    val accelerations: List<Acceleration>
)

@OptIn(ExperimentalCoroutinesApi::class)
object AccelerationDebugger {
    private val logger = KotlinLogging.logger {}
    private const val HISTORY_LENGTH = 333

    private val accelerationHistory: MutableMap<MacAddress, FixedSizeQueue<Pair<Long, Double>>> = ConcurrentHashMap()

    private fun getHistoryByMac(mac: MacAddress): FixedSizeQueue<Pair<Long, Double>> {
        return accelerationHistory[mac] ?: let {
            val history = FixedSizeQueue<Pair<Long, Double>>(HISTORY_LENGTH)
            accelerationHistory[mac] = history
            return history
        }
    }

    init {
        CoroutineScope(CustomThreadDispatcher.DEBUG_UI).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
                newMoves.asFlow().flatMapMerge { move ->
                    move.accelerationFlow.map { accelerationData -> Pair(move.macAddress, accelerationData.change) }
                }
            }.collect { accelerationData ->
                val currentTime = System.currentTimeMillis()
                getHistoryByMac(accelerationData.first).add(Pair(currentTime, accelerationData.second))
            }
        }

        CoroutineScope(CustomThreadDispatcher.DEBUG_UI).launch {
            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.onlyRemovedFromPrevious().collect { moves ->
                moves.forEach { move ->
                    accelerationHistory.remove(move.macAddress)
                }
            }
        }
    }

    val psMoveStubStatistics = PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
        combine(newMoves.map { stub -> stub.moveStatisticsFlow.map { statistics -> stub.macAddress to statistics } }) { pairs ->
            pairs.associate { it }
        }
    }

    init {
        CoroutineScope(CustomThreadDispatcher.DEBUG_UI).launch {
            psMoveStubStatistics.collect { stats ->
                logger.trace { "PSMove stats: $stats" }
            }
        }
    }

    fun getHistoryAsJson(): String {
        val result: List<StubAcceleration> = accelerationHistory.map { (mac, history) ->
            StubAcceleration(
                mac = mac,
                accelerations = history.toList().map { (timestamp, change) ->
                    Acceleration(t = timestamp, a = change)
                }
            )
        }

        return Json.encodeToString(result)
    }
}