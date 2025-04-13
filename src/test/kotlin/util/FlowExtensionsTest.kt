package util

import de.vanfanel.joustmania.util.onlyAddedFromPrevious
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import de.vanfanel.joustmania.util.onlyRemovedFromPrevious
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlowExtensionsTest {

    @Test
    fun `onlyRemovedFromPrevious returns correct removed elements`(): Unit = runTest {
        val input = flowOf(
            setOf("A", "B"),
            setOf("A", "B", "C"),
            setOf("B", "C"),
            setOf("C", "D"),
            setOf(),
            setOf("A"),
            setOf()
        )

        val expected = listOf(
            setOf("A"),
            setOf("B"),
            setOf("C", "D"),
            setOf("A"),
        )

        val actual = input.onlyRemovedFromPrevious().toList()

        assertEquals(expected, actual)
    }

    @Test
    fun `onlyAddedFromPrevious returns correct added elements`() = runTest {
        val flow = flowOf(
            setOf("A", "B"),
            setOf("A", "B", "C"),
            setOf("B", "C"),
            setOf("C", "D"),
            setOf(),
            setOf("A")
        )

        val addedElements = mutableListOf<Set<String>>()

        flow.onlyAddedFromPrevious().collect { added ->
            addedElements.add(added)
        }

        assertEquals(listOf(
            setOf("A", "B"),
            setOf("C"),
            setOf("D"),
            setOf("A")
        ), addedElements )
    }
}
