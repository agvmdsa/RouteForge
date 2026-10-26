package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.Route
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LastComputedRouteHandoffTest {
    private val first = Route(points = emptyList(), geometry = emptyList(), distanceMeters = 1.0)
    private val second = Route(points = emptyList(), geometry = emptyList(), distanceMeters = 2.0)

    @Test
    fun `no route is known at the start`() {
        val reader: LastComputedRouteHandoffReader = LastComputedRouteHandoffImpl()

        assertNull(reader.route.value)
    }

    @Test
    fun `a route set by the writer is what the reader reads`() {
        val handoff = LastComputedRouteHandoffImpl()
        val writer: LastComputedRouteHandoffWriter = handoff
        val reader: LastComputedRouteHandoffReader = handoff

        writer.set(first)

        assertEquals(first, reader.route.value)
    }

    @Test
    fun `when the writer sets twice the reader sees the second route`() {
        val handoff = LastComputedRouteHandoffImpl()

        handoff.set(first)
        handoff.set(second)

        assertEquals(second, handoff.route.value)
    }

    @Test
    fun `a reader that starts collecting after a write gets the current route at once`() =
        runTest {
            val handoff = LastComputedRouteHandoffImpl()

            handoff.set(first)

            assertEquals(first, handoff.route.first())
        }

    @Test
    fun `clearing leaves no route for the reader after one was set`() {
        val handoff = LastComputedRouteHandoffImpl()
        val clearer: LastComputedRouteHandoffClearer = handoff
        handoff.set(first)

        clearer.clear()

        assertNull(handoff.route.value)
    }

    @Test
    fun `a reader that is already collecting sees each route the writer sets and the clear`() =
        runTest {
            val handoff = LastComputedRouteHandoffImpl()
            val seen = mutableListOf<Route?>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { handoff.route.toList(seen) }

            handoff.set(first)
            handoff.set(second)
            handoff.clear()

            assertEquals(listOf(null, first, second, null), seen)
        }
}
