package de.vanfanel.joustmania.util

import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory


object CustomThreadDispatcher {
    val POLLING = Executors.newSingleThreadExecutor(NamedThreadFactory("MovePollThread")).asCoroutineDispatcher()
    val GAME_LOOP = Executors.newSingleThreadExecutor(NamedThreadFactory("GameLoopCoroutineThread")).asCoroutineDispatcher()
    val GAME_LOGIC = Executors.newSingleThreadExecutor(NamedThreadFactory("GameLogicCoroutineThread")).asCoroutineDispatcher()
    val SOUND = Executors.newSingleThreadExecutor(NamedThreadFactory("SoundCoroutineThread")).asCoroutineDispatcher()

    fun shutdown() {
        POLLING.close()
        GAME_LOOP.close()
        GAME_LOGIC.close()
        SOUND.close()
    }
}

class NamedThreadFactory(private val baseName: String) : ThreadFactory {
    private var counter = 0

    override fun newThread(r: Runnable): Thread {
        return Thread(r, "$baseName-${counter++}")
    }
}