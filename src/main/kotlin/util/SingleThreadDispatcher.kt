package de.vanfanel.joustmania.util

import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory


object SingleThreadDispatcher {
    val BUTTONS = Executors.newSingleThreadExecutor(NamedThreadFactory("ButtonsCoroutineThread")).asCoroutineDispatcher()
    val COLORS = Executors.newSingleThreadExecutor(NamedThreadFactory("ColorsCoroutineThread")).asCoroutineDispatcher()
    val MOVEMENT = Executors.newSingleThreadExecutor(NamedThreadFactory("MovementCoroutineThread")).asCoroutineDispatcher()
    val GAME_LOOP = Executors.newSingleThreadExecutor(NamedThreadFactory("GameLoopCoroutineThread")).asCoroutineDispatcher()

    fun shutdown() {
        BUTTONS.close()
        COLORS.close()
        MOVEMENT.close()
        GAME_LOOP.close()
    }
}

// ThreadFactory, um benannte Threads zu erstellen
class NamedThreadFactory(private val baseName: String) : ThreadFactory {
    private var counter = 0

    override fun newThread(r: Runnable): Thread {
        return Thread(r, "$baseName-${counter++}")
    }
}