package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.RealLocation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LastKnownRealLocationHandoffTest {
    private val home = RealLocation(latitude = 52.5, longitude = 13.4)
    private val work = RealLocation(latitude = 48.1, longitude = 11.5)

    @Test
    fun `no location is known at the start`() {
        val reader: LastKnownRealLocationHandoffReader = LastKnownRealLocationHandoffImpl()

        assertNull(reader.location.value)
    }

    @Test
    fun `a location set by the writer is what the reader reads`() {
        val handoff = LastKnownRealLocationHandoffImpl()
        val writer: LastKnownRealLocationHandoffWriter = handoff
        val reader: LastKnownRealLocationHandoffReader = handoff

        writer.set(home)

        assertEquals(home, reader.location.value)
    }

    @Test
    fun `when the writer sets twice the reader sees the second location`() {
        val handoff = LastKnownRealLocationHandoffImpl()

        handoff.set(home)
        handoff.set(work)

        assertEquals(work, handoff.location.value)
    }

    @Test
    fun `a reader that starts collecting after a write gets the current location at once`() =
        runTest {
            val handoff = LastKnownRealLocationHandoffImpl()

            handoff.set(home)

            assertEquals(home, handoff.location.first())
        }

    @Test
    fun `a reader that is already collecting sees each location the writer sets`() =
        runTest {
            val handoff = LastKnownRealLocationHandoffImpl()
            val seen = mutableListOf<RealLocation?>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { handoff.location.toList(seen) }

            handoff.set(home)
            handoff.set(work)

            assertEquals(listOf(null, home, work), seen)
        }
}
