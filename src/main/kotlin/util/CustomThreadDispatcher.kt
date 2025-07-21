package de.vanfanel.joustmania.util

import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory


object CustomThreadDispatcher {
    val POLLING = Executors.newSingleThreadExecutor(NamedThreadFactory("MovePollThread")).asCoroutineDispatcher()
    val COLOR_CALCULATION = Executors.newSingleThreadExecutor(NamedThreadFactory("ColorsCoroutineThread")).asCoroutineDispatcher()
    val GAME_LOOP = Executors.newSingleThreadExecutor(NamedThreadFactory("GameLoopCoroutineThread")).asCoroutineDispatcher()

    fun shutdown() {
        POLLING.close()
        COLOR_CALCULATION.close()
        GAME_LOOP.close()
    }
}

class NamedThreadFactory(private val baseName: String) : ThreadFactory {
    private var counter = 0

    override fun newThread(r: Runnable): Thread {
        return Thread(r, "$baseName-${counter++}")
    }
}