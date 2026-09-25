package com.routeforge.simulation.data

import com.routeforge.coredomain.LastKnownRealLocationHolder
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.RealLocation
import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.ExecutionModeFailure
import com.routeforge.simulation.domain.JoystickStartFailure
import com.routeforge.simulation.domain.model.ExecutionMode
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationStatus
import com.routeforge.simulation.domain.model.SpeedSetting
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
    private val lastKnownRealLocationHolder = LastKnownRealLocationHolder()

    private fun createController(): SimulationControllerImpl =
        SimulationControllerImpl(
            mockLocationPublisher = publisher,
            mockLocationAuthorizationChecker = authorizationChecker,
            lastKnownRealLocationHolder = lastKnownRealLocationHolder,
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

        controller.startRoute(longRoute, speedSetting = SpeedSetting.Manual(10f))
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

        controller.startRoute(shortRoute, speedSetting = SpeedSetting.Manual(100f))
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

        controller.startRoute(longRoute, speedSetting = SpeedSetting.Manual(10f))
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
        controller.startRoute(longRoute, speedSetting = SpeedSetting.Manual(10f))

        authorizationChecker.authorized = false
        controller.tick(elapsedSeconds = 1.0)

        assertNull(controller.mockedSession.value)
        assertEquals(1, publisher.clearCallCount)
    }

    // --- User Story 4: speed control ---

    @Test
    fun `setSpeed updates the active session's effective speed immediately without resetting progress`() {
        val controller = createController()
        controller.startRoute(longRoute, speedSetting = SpeedSetting.Manual(10f))
        controller.tick(elapsedSeconds = 1.0)
        val distanceBefore = controller.mockedSession.value?.distanceTraveledMeters

        controller.setSpeed(SpeedSetting.Manual(50f))

        assertEquals(distanceBefore, controller.mockedSession.value?.distanceTraveledMeters)
        assertEquals(50f, controller.mockedSession.value?.speedMetersPerSecond)
        controller.tick(elapsedSeconds = 1.0)
        assertEquals((distanceBefore ?: 0.0) + 50.0, controller.mockedSession.value?.distanceTraveledMeters)
    }

    // --- User Story 6: execution mode ---

    @Test
    fun `setExecutionMode rejects a non-positive Times count`() {
        val controller = createController()

        val result = controller.setExecutionMode(ExecutionMode.Times(0))

        assertEquals(Result.Error(ExecutionModeFailure.NonPositiveCount), result)
    }

    @Test
    fun `a route set to Times restarts automatically until the count is reached, then stops`() {
        val controller = createController()
        controller.startRoute(shortRoute, speedSetting = SpeedSetting.Manual(100f))
        controller.setExecutionMode(ExecutionMode.Times(2))

        controller.tick(elapsedSeconds = 1.0) // completes lap 1, restarts
        assertEquals(SimulationStatus.RUNNING, controller.mockedSession.value?.status)
        assertEquals(1, controller.mockedSession.value?.completedRuns)
        assertEquals(0.0, controller.mockedSession.value?.distanceTraveledMeters)
        assertEquals(SpeedSetting.Manual(100f), controller.mockedSession.value?.speedSetting)

        controller.tick(elapsedSeconds = 1.0) // completes lap 2, stops (count reached)
        assertEquals(SimulationStatus.COMPLETED, controller.mockedSession.value?.status)
    }

    @Test
    fun `a route set to Loop restarts at least ten consecutive times without manual intervention`() {
        val controller = createController()
        controller.startRoute(shortRoute, speedSetting = SpeedSetting.Manual(100f))
        controller.setExecutionMode(ExecutionMode.Loop)

        repeat(10) { controller.tick(elapsedSeconds = 1.0) }

        assertEquals(SimulationStatus.RUNNING, controller.mockedSession.value?.status)
        assertEquals(10, controller.mockedSession.value?.completedRuns)
        assertEquals(shortRoute.geometry.first().first, controller.mockedSession.value?.latitude)
    }

    // --- User Story 7: joystick ---

    @Test
    fun `startJoystick fails when no real fix has ever been observed and no session exists`() {
        val controller = createController()

        val result = controller.startJoystick()

        assertEquals(Result.Error(JoystickStartFailure.NoRealLocationFixYet), result)
        assertNull(controller.mockedSession.value)
    }

    @Test
    fun `startJoystick succeeds from the last known real location when no session exists`() {
        lastKnownRealLocationHolder.set(RealLocation(latitude = 5.0, longitude = 6.0))
        val controller = createController()

        val result = controller.startJoystick()

        assertEquals(Result.Success(Unit), result)
        assertEquals(SimulationMode.JOYSTICK, controller.mockedSession.value?.mode)
        assertEquals(5.0, controller.mockedSession.value?.latitude)
        assertEquals(6.0, controller.mockedSession.value?.longitude)
    }

    @Test
    fun `startJoystick while a route is active hands off from the route's current position`() {
        val controller = createController()
        controller.startRoute(longRoute, speedSetting = SpeedSetting.Manual(10f))
        controller.tick(elapsedSeconds = 1.0)
        val positionDuringRoute = controller.mockedSession.value!!.let { it.latitude to it.longitude }

        val result = controller.startJoystick()

        assertEquals(Result.Success(Unit), result)
        assertEquals(SimulationMode.JOYSTICK, controller.mockedSession.value?.mode)
        assertEquals(positionDuringRoute, controller.mockedSession.value?.let { it.latitude to it.longitude })
    }

    @Test
    fun `joystick moves continuously in the direction set by updateJoystickDirection`() {
        lastKnownRealLocationHolder.set(RealLocation(latitude = 0.0, longitude = 0.0))
        val controller = createController()
        controller.startJoystick()
        controller.setSpeed(SpeedSetting.Manual(10f))

        controller.updateJoystickDirection(bearingDegrees = 0f)
        controller.tick(elapsedSeconds = 1.0)

        val moved = controller.mockedSession.value!!
        assertTrue(moved.latitude > 0.0)
        assertEquals(0.0, moved.longitude, 0.0000001)
    }

    @Test
    fun `releasing the joystick via pause stops movement in place`() {
        lastKnownRealLocationHolder.set(RealLocation(latitude = 0.0, longitude = 0.0))
        val controller = createController()
        controller.startJoystick()
        controller.setSpeed(SpeedSetting.Manual(10f))
        controller.updateJoystickDirection(bearingDegrees = 0f)
        controller.tick(elapsedSeconds = 1.0)
        val positionAtRelease = controller.mockedSession.value!!.let { it.latitude to it.longitude }

        controller.pause()
        controller.tick(elapsedSeconds = 1.0)

        assertEquals(SimulationStatus.PAUSED, controller.mockedSession.value?.status)
        assertEquals(positionAtRelease, controller.mockedSession.value?.let { it.latitude to it.longitude })
    }
}
