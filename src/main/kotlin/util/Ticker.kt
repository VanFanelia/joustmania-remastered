package de.vanfanel.joustmania.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

class Ticker(private val interval: Duration) {

    private val _state = MutableStateFlow(false) // True = läuft, False = gestoppt
    private val _tickerFlow = MutableSharedFlow<Long>(replay = 0)

    private var job: Job? = null

    // public flow which emits the ticks
    val tick: Flow<Long> = _tickerFlow

    fun start() {
        if (_state.value) return // Bereits gestartet
        _state.value = true

        job = CoroutineScope(Dispatchers.IO).launch {
            var count = 0L
            while (_state.value) {
                _tickerFlow.emit(count++)
                delay(interval)
            }
        }
    }

    fun stop() {
        _state.value = false
        job?.cancel()
        job = null
    }

    @Suppress("unused")
    fun restart() {
        stop()
        start()
    }
}