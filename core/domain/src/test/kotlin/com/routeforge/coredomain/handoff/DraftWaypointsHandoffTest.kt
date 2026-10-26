package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DraftWaypointsHandoffTest {
    private val home = listOf(RoutePoint(52.5, 13.4), RoutePoint(52.6, 13.5))
    private val work = listOf(RoutePoint(48.1, 11.5))

    @Test
    fun `the draft starts empty`() {
        val reader: DraftWaypointsHandoffReader = DraftWaypointsHandoffImpl()

        assertEquals(emptyList<RoutePoint>(), reader.points.value)
    }

    @Test
    fun `a value set by the writer is what the reader reads`() {
        val handoff = DraftWaypointsHandoffImpl()
        val writer: DraftWaypointsHandoffWriter = handoff
        val reader: DraftWaypointsHandoffReader = handoff

        writer.set(home)

        assertEquals(home, reader.points.value)
    }

    @Test
    fun `when the writer sets twice the reader sees the second value`() {
        val handoff = DraftWaypointsHandoffImpl()

        handoff.set(home)
        handoff.set(work)

        assertEquals(work, handoff.points.value)
    }

    @Test
    fun `a reader that starts collecting after a write gets the current value at once`() =
        runTest {
            val handoff = DraftWaypointsHandoffImpl()

            handoff.set(home)

            assertEquals(home, handoff.points.first())
        }

    @Test
    fun `a reader that is already collecting sees each value the writer sets`() =
        runTest {
            val handoff = DraftWaypointsHandoffImpl()
            val seen = mutableListOf<List<RoutePoint>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { handoff.points.toList(seen) }

            handoff.set(home)
            handoff.set(work)

            assertEquals(listOf(emptyList(), home, work), seen)
        }
}
