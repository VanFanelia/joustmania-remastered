package de.vanfanel.joustmania.hardware

import de.vanfanel.joustmania.hardware.psmove.PSMoveBluetoothConnectionWatcher
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.util.FixedSizeQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    private const val HISTORY_LENGTH = 1000

    private var accelerationDebuggerJob: Job? = null
    private val accelerationHistory: MutableMap<MacAddress, FixedSizeQueue<Pair<Long, Double>>> = mutableMapOf()

    private fun getHistoryByMac(mac: MacAddress): FixedSizeQueue<Pair<Long, Double>> {
        return accelerationHistory[mac] ?: let {
            val history = FixedSizeQueue<Pair<Long, Double>>(HISTORY_LENGTH)
            accelerationHistory[mac] = history
            return history
        }
    }

    init {
        accelerationDebuggerJob = CoroutineScope(Dispatchers.IO).launch {

            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
                newMoves.asFlow().flatMapMerge { move ->
                    move.accelerationFlow.map { accelerationData -> Pair(move.macAddress, accelerationData.change) }
                }
            }.collect { accelerationData ->
                val currentTime = System.currentTimeMillis()
                getHistoryByMac(accelerationData.first).add(Pair(currentTime, accelerationData.second))
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