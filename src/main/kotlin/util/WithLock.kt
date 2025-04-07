package de.vanfanel.joustmania.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

suspend fun <T> withLock(lock: Mutex, block: suspend () -> T): T {
    return lock.withLock {
        block()
    }
}