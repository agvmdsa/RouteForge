package com.routeforge.coredomain

import com.routeforge.coredomain.model.TourSection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TourReplayCoordinatorTest {
    private val coordinator = TourReplayCoordinator()

    @Test
    fun `nothing is pending until a replay is requested`() {
        assertNull(coordinator.pendingReplay.value)
    }

    @Test
    fun `a requested replay stays pending until its own section consumes it`() {
        coordinator.requestReplay(TourSection.SIMULATION)

        assertEquals(TourSection.SIMULATION, coordinator.pendingReplay.value)

        coordinator.consumeReplay(TourSection.SIMULATION)

        assertNull(coordinator.pendingReplay.value)
    }

    @Test
    fun `another section consuming does not take the request away`() {
        coordinator.requestReplay(TourSection.SIMULATION)

        coordinator.consumeReplay(TourSection.ROUTE_CREATION)

        assertEquals(TourSection.SIMULATION, coordinator.pendingReplay.value)
    }

    @Test
    fun `consuming when nothing is pending changes nothing`() {
        coordinator.consumeReplay(TourSection.ROUTE_CREATION)

        assertNull(coordinator.pendingReplay.value)
    }

    @Test
    fun `a second request before consumption replaces the first`() {
        coordinator.requestReplay(TourSection.SIMULATION)
        coordinator.requestReplay(TourSection.ROUTE_CREATION)

        assertEquals(TourSection.ROUTE_CREATION, coordinator.pendingReplay.value)

        coordinator.consumeReplay(TourSection.SIMULATION)

        assertEquals(TourSection.ROUTE_CREATION, coordinator.pendingReplay.value)
    }

    @Test
    fun `a screen that starts observing after the request still receives it`() =
        runBlocking {
            coordinator.requestReplay(TourSection.ROUTE_CREATION)

            val received = withTimeout(1_000) { coordinator.pendingReplay.first { it != null } }

            assertEquals(TourSection.ROUTE_CREATION, received)
        }

    @Test
    fun `the same section can be replayed again once it was consumed`() {
        coordinator.requestReplay(TourSection.ROUTE_CREATION)
        coordinator.consumeReplay(TourSection.ROUTE_CREATION)

        coordinator.requestReplay(TourSection.ROUTE_CREATION)

        assertEquals(TourSection.ROUTE_CREATION, coordinator.pendingReplay.value)
    }

    @Test
    fun `finished replays are delivered in order even when reported before anyone collects`() =
        runBlocking {
            coordinator.notifyReplayFinished(TourSection.SIMULATION)
            coordinator.notifyReplayFinished(TourSection.ROUTE_CREATION)

            val first = withTimeout(1_000) { coordinator.replayCompletions.first() }

            assertEquals(TourSection.SIMULATION, first)
        }

    @Test
    fun `a finish left over from an earlier run is dropped when the next replay is requested`() =
        runBlocking {
            // An automatic first-visit tour (or a single Repeat nobody was waiting on) finishing.
            coordinator.notifyReplayFinished(TourSection.SIMULATION)

            coordinator.requestReplay(TourSection.SIMULATION)
            coordinator.notifyReplayFinished(TourSection.ROUTE_CREATION)

            val first = withTimeout(1_000) { coordinator.replayCompletions.first() }

            assertEquals(TourSection.ROUTE_CREATION, first)
        }

    @Test
    fun `a finish reported after a replay was requested is still delivered`() =
        runBlocking {
            coordinator.requestReplay(TourSection.ROUTE_CREATION)
            coordinator.notifyReplayFinished(TourSection.ROUTE_CREATION)

            val first = withTimeout(1_000) { coordinator.replayCompletions.first() }

            assertEquals(TourSection.ROUTE_CREATION, first)
        }

    @Test
    fun `consuming a plain request reports that it does not continue a sequence`() {
        coordinator.requestReplay(TourSection.SIMULATION)

        assertFalse(coordinator.consumeReplay(TourSection.SIMULATION))
    }

    @Test
    fun `consuming a request that continues a sequence reports it`() {
        coordinator.requestReplay(TourSection.ROUTE_CREATION, continuesSequence = true)

        assertTrue(coordinator.consumeReplay(TourSection.ROUTE_CREATION))
    }

    @Test
    fun `consuming nothing or another section's request reports false and keeps the request`() {
        assertFalse(coordinator.consumeReplay(TourSection.ROUTE_CREATION))

        coordinator.requestReplay(TourSection.SIMULATION, continuesSequence = true)

        assertFalse(coordinator.consumeReplay(TourSection.ROUTE_CREATION))
        assertEquals(TourSection.SIMULATION, coordinator.pendingReplay.value)
        assertTrue(coordinator.consumeReplay(TourSection.SIMULATION))
    }

    @Test
    fun `a sequence request is only reported once`() {
        coordinator.requestReplay(TourSection.ROUTE_CREATION, continuesSequence = true)
        coordinator.consumeReplay(TourSection.ROUTE_CREATION)

        coordinator.requestReplay(TourSection.ROUTE_CREATION)

        assertFalse(coordinator.consumeReplay(TourSection.ROUTE_CREATION))
    }

    @Test
    fun `a newer request replaces whether the older one continued a sequence`() {
        coordinator.requestReplay(TourSection.ROUTE_CREATION, continuesSequence = true)
        coordinator.requestReplay(TourSection.ROUTE_CREATION)

        assertFalse(coordinator.consumeReplay(TourSection.ROUTE_CREATION))
    }
}
