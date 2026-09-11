package com.routeforge.simulation.data

import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val longRoute =
    Route(
        points = emptyList(),
        geometry = listOf(0.0 to 0.0, 10.0 to 0.0),
        distanceMeters = 1_000_000.0,
    )

private val shortRoute =
    Route(
        points = emptyList(),
        geometry = listOf(0.0 to 0.0, 0.0 to 0.0001),
        distanceMeters = 11.0,
    )

class SimulationControllerImplTest {
    private val backgroundJob = Job().apply { cancel() }
    private val backgroundScope = CoroutineScope(backgroundJob)
    private val publisher = FakeMockLocationPublisher()
    private val authorizationChecker = FakeMockLocationAuthorizationChecker()

    private fun createController(): SimulationControllerImpl =
        SimulationControllerImpl(
            mockLocationPublisher = publisher,
            mockLocationAuthorizationChecker = authorizationChecker,
            coroutineScope = backgroundScope,
        )

    @Test
    fun `teleport immediately publishes the point once and reports a stationary running session`() {
        val controller = createController()

        controller.teleport(latitude = 12.0, longitude = 34.0)

        val mockedSession = controller.mockedSession.value
        assertEquals(SimulationMode.STATIONARY, mockedSession?.mode)
        assertEquals(SimulationStatus.RUNNING, mockedSession?.status)
        assertEquals(12.0, mockedSession?.latitude)
        assertEquals(34.0, mockedSession?.longitude)
        assertEquals(1, publisher.publishedFixes.size)
        assertEquals(12.0, publisher.publishedFixes.first().latitude)
    }

    @Test
    fun `startRoute advances distance traveled by speed times elapsed time on each tick`() {
        val controller = createController()

        controller.startRoute(longRoute, speedMetersPerSecond = 10f)
        controller.tick(elapsedSeconds = 1.0)
        controller.tick(elapsedSeconds = 1.0)

        val mockedSession = controller.mockedSession.value
        assertEquals(SimulationMode.ROUTE, mockedSession?.mode)
        assertEquals(SimulationStatus.RUNNING, mockedSession?.status)
        assertEquals(20.0, mockedSession?.distanceTraveledMeters)
    }

    @Test
    fun `a route reaching its end completes and keeps reporting the final point rather than stopping or looping`() {
        val controller = createController()

        controller.startRoute(shortRoute, speedMetersPerSecond = 100f)
        controller.tick(elapsedSeconds = 1.0)

        val completedSession = controller.mockedSession.value
        assertEquals(SimulationStatus.COMPLETED, completedSession?.status)
        assertEquals(0.0001, completedSession?.longitude)
        val distanceAtCompletion = completedSession?.distanceTraveledMeters

        controller.tick(elapsedSeconds = 1.0)
        controller.tick(elapsedSeconds = 1.0)

        val stillCompletedSession = controller.mockedSession.value
        assertEquals(SimulationStatus.COMPLETED, stillCompletedSession?.status)
        assertEquals(distanceAtCompletion, stillCompletedSession?.distanceTraveledMeters)
        assertEquals(0.0001, stillCompletedSession?.longitude)
        assertTrue(publisher.publishedFixes.size >= 3)
    }

    @Test
    fun `pause halts distance progression and resume continues from the paused distance`() {
        val controller = createController()

        controller.startRoute(longRoute, speedMetersPerSecond = 10f)
        controller.tick(elapsedSeconds = 1.0)
        controller.pause()
        val distanceWhenPaused = controller.mockedSession.value?.distanceTraveledMeters

        controller.tick(elapsedSeconds = 1.0)
        assertEquals(SimulationStatus.PAUSED, controller.mockedSession.value?.status)
        assertEquals(distanceWhenPaused, controller.mockedSession.value?.distanceTraveledMeters)

        controller.resume()
        assertEquals(SimulationStatus.RUNNING, controller.mockedSession.value?.status)
        controller.tick(elapsedSeconds = 1.0)

        assertEquals((distanceWhenPaused ?: 0.0) + 10.0, controller.mockedSession.value?.distanceTraveledMeters)
    }

    @Test
    fun `stop clears the session and clears the mock location publisher`() {
        val controller = createController()
        controller.teleport(latitude = 1.0, longitude = 1.0)

        controller.stop()

        assertNull(controller.mockedSession.value)
        assertEquals(1, publisher.clearCallCount)
    }

    @Test
    fun `revoked authorization mid-simulation stops the session cleanly on the next tick`() {
        val controller = createController()
        controller.startRoute(longRoute, speedMetersPerSecond = 10f)

        authorizationChecker.authorized = false
        controller.tick(elapsedSeconds = 1.0)

        assertNull(controller.mockedSession.value)
        assertEquals(1, publisher.clearCallCount)
    }
}
