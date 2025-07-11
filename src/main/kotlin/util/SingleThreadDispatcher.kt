package de.vanfanel.joustmania.util

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors


object SingleThreadDispatcher {
    val BUTTONS = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val COLORS = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val MOVEMENT = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    fun shutdown() {
        BUTTONS.close()
        COLORS.close()
        MOVEMENT.close()
    }
}