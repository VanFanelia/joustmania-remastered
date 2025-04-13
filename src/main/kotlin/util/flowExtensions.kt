package de.vanfanel.joustmania.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan


fun <T> Flow<Set<T>>.onlyRemovedFromPrevious(): Flow<Set<T>> =
    this.scan(emptySet<T>() to emptySet()) { acc: Pair<Set<T>, Set<T>>, current: Set<T> ->
        val previous = acc.first
        val removed = previous - current
        current to removed
    }.drop(1)
        .map { it.second }.filter { it.isNotEmpty() }

fun <T> Flow<Set<T>>.onlyAddedFromPrevious(): Flow<Set<T>> {
    var lastValue: Set<T> = emptySet()
    return this.map { current ->
        val result = current - lastValue
        lastValue = current
        result
    }.filter { it.isNotEmpty() }
}
