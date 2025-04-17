package de.vanfanel.joustmania.util

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FixedSizeQueue<T>(private val maxSize: Int) {
    private val deque = ArrayDeque<T>()
    private val lock = ReentrantLock()


    fun add(element: T) {
        lock.withLock {
            if (deque.size == maxSize) {
                deque.removeFirst()
            }
            deque.addLast(element)
        }
    }

    fun toList(): List<T> =
        deque.toList()


    val size: Int
        get() = deque.size

}