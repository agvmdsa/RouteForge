package com.routeforge.simulation.presentation

import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SimulationStatus
import com.routeforge.simulation.domain.model.SpeedSetting
import com.routeforge.simulation.domain.testing.FakeSimulationController
import com.routeforge.simulation.domain.usecase.PauseSimulationUseCase
import com.routeforge.simulation.domain.usecase.ResumeSimulationUseCase
import com.routeforge.simulation.domain.usecase.SetSpeedUseCase
import com.routeforge.simulation.domain.usecase.StopSimulationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackControlsTest {
    private val controller = FakeSimulationController()
    private val lastComputedRouteHolder = LastComputedRouteHolder()
    private val playbackControls =
        PlaybackControls(
            pauseSimulation = PauseSimulationUseCase(controller),
            resumeSimulation = ResumeSimulationUseCase(controller),
            stopSimulation = StopSimulationUseCase(controller),
            setSpeed = SetSpeedUseCase(controller),
            lastComputedRouteHolder = lastComputedRouteHolder,
        )

    private val sampleRoute =
        Route(
            points = emptyList(),
            geometry = listOf(LatLng(0.0, 0.0), LatLng(1.0, 1.0)),
            distanceMeters = 100.0,
        )

    private val activeSession =
        SimulationSession(
            mode = SimulationMode.ROUTE,
            status = SimulationStatus.RUNNING,
            latitude = 10.0,
            longitude = 20.0,
            bearingDegrees = 0f,
            speedMetersPerSecond = 0f,
            route = sampleRoute,
            distanceTraveledMeters = 0.0,
        )

    @Test
    fun `pausing reaches the controller`() {
        playbackControls.pause()

        assertEquals(1, controller.pauseCallCount)
    }

    @Test
    fun `resuming reaches the controller`() {
        playbackControls.resume()

        assertEquals(1, controller.resumeCallCount)
    }

    @Test
    fun `stopping the route clears the route, stops the session and closes the cancel dialog`() {
        lastComputedRouteHolder.set(sampleRoute)
        val state = MutableStateFlow(SimulationState(loadedRoute = sampleRoute, isPendingCancelRoute = true))

        playbackControls.stop(state)

        assertNull(lastComputedRouteHolder.route.value)
        assertEquals(1, controller.stopCallCount)
        assertFalse(state.value.isPendingCancelRoute)
    }

    @Test
    fun `confirming cancel-mock stops the session and closes the cancel dialog`() {
        val state = MutableStateFlow(SimulationState(mockedSession = activeSession, isPendingCancelMock = true))

        playbackControls.confirmCancelMock(state)

        assertEquals(1, controller.stopCallCount)
        assertFalse(state.value.isPendingCancelMock)
    }

    @Test
    fun `confirming cancel-mock keeps the loaded route`() {
        lastComputedRouteHolder.set(sampleRoute)
        val state = MutableStateFlow(SimulationState(loadedRoute = sampleRoute, isPendingCancelMock = true))

        playbackControls.confirmCancelMock(state)

        assertEquals(sampleRoute, lastComputedRouteHolder.route.value)
    }

    @Test
    fun `a speed change with no session only stages the speed`() {
        val state = MutableStateFlow(SimulationState())

        playbackControls.changePlaybackSpeed(36f, state)

        assertEquals(36f, state.value.playbackSpeedKmh)
        assertTrue(controller.setSpeedCalls.isEmpty())
    }

    @Test
    fun `a speed change while a session is active reaches the controller in meters per second`() {
        val state = MutableStateFlow(SimulationState(mockedSession = activeSession))

        playbackControls.changePlaybackSpeed(36f, state)

        assertEquals(36f, state.value.playbackSpeedKmh)
        assertEquals(listOf(SpeedSetting.Manual(10f)), controller.setSpeedCalls)
    }
}
