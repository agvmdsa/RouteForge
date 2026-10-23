package com.routeforge.simulation.presentation

import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.testing.FakeMockLocationAuthorizationChecker
import com.routeforge.simulation.domain.model.ExecutionMode
import com.routeforge.simulation.domain.model.SpeedSetting
import com.routeforge.simulation.domain.testing.FakeSimulationController
import com.routeforge.simulation.domain.usecase.ObserveAuthorizationLostUseCase
import com.routeforge.simulation.domain.usecase.StartRouteSimulationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RouteStarterTest {
    private val controller = FakeSimulationController()
    private val authorizationChecker = FakeMockLocationAuthorizationChecker()
    private val starter =
        RouteStarter(
            startRouteSimulationUseCase = StartRouteSimulationUseCase(controller),
            mockLocationAuthorizationGuard =
                MockLocationAuthorizationGuard(authorizationChecker, ObserveAuthorizationLostUseCase(controller)),
        )

    private val sampleRoute =
        Route(
            points = emptyList(),
            geometry = listOf(LatLng(0.0, 0.0), LatLng(1.0, 1.0)),
            distanceMeters = 100.0,
        )

    private fun dialogOpen(
        playbackSpeedKmh: Float = 36f,
        executionModeSelection: ExecutionModeSelection = ExecutionModeSelection.ONCE,
        executionTimesInput: String = "2",
    ) = MutableStateFlow(
        SimulationState(
            loadedRoute = sampleRoute,
            isStartRouteDialogOpen = true,
            playbackSpeedKmh = playbackSpeedKmh,
            executionModeSelection = executionModeSelection,
            executionTimesInput = executionTimesInput,
        ),
    )

    @Test
    fun `confirming starts the route at the chosen speed in meters per second and closes the dialog`() {
        val state = dialogOpen(playbackSpeedKmh = 36f)

        starter.confirmStartRoute(state)

        assertEquals(1, controller.startRouteCalls.size)
        assertEquals(sampleRoute, controller.startRouteCalls.first().route)
        assertEquals(SpeedSetting.Manual(10f), controller.startRouteCalls.first().speedSetting)
        assertEquals(ExecutionMode.Once, controller.startRouteCalls.first().executionMode)
        assertFalse(state.value.isStartRouteDialogOpen)
        assertNull(state.value.errorType)
    }

    @Test
    fun `confirming with the times mode starts with that number of times`() {
        val state = dialogOpen(executionModeSelection = ExecutionModeSelection.TIMES, executionTimesInput = "3")

        starter.confirmStartRoute(state)

        assertEquals(ExecutionMode.Times(3), controller.startRouteCalls.first().executionMode)
    }

    @Test
    fun `a speed of zero closes the dialog with the invalid speed error and starts nothing`() {
        val state = dialogOpen(playbackSpeedKmh = 0f)

        starter.confirmStartRoute(state)

        assertTrue(controller.startRouteCalls.isEmpty())
        assertEquals(SimulationErrorType.INVALID_SPEED, state.value.errorType)
        assertFalse(state.value.isStartRouteDialogOpen)
    }

    @Test
    fun `a times count that is not a positive number keeps the dialog open with the invalid times error`() {
        val state = dialogOpen(executionModeSelection = ExecutionModeSelection.TIMES, executionTimesInput = "0")

        starter.confirmStartRoute(state)

        assertTrue(controller.startRouteCalls.isEmpty())
        assertEquals(SimulationErrorType.INVALID_EXECUTION_TIMES, state.value.errorType)
        assertTrue(state.value.isStartRouteDialogOpen)
    }

    @Test
    fun `confirming while unauthorized closes the dialog and reports the authorization error`() {
        authorizationChecker.authorized = false
        val state = dialogOpen()

        starter.confirmStartRoute(state)

        assertTrue(controller.startRouteCalls.isEmpty())
        assertEquals(SimulationErrorType.NOT_AUTHORIZED, state.value.errorType)
        assertTrue(state.value.isBlockedByAuthorization)
        assertFalse(state.value.isStartRouteDialogOpen)
    }

    @Test
    fun `confirming with no route loaded does nothing`() {
        val state = MutableStateFlow(SimulationState(isStartRouteDialogOpen = true))

        starter.confirmStartRoute(state)

        assertTrue(controller.startRouteCalls.isEmpty())
        assertTrue(state.value.isStartRouteDialogOpen)
    }

    @Test
    fun `each execution mode resolves to its domain mode`() {
        assertEquals(ExecutionMode.Once, resolveExecutionMode(ExecutionModeSelection.ONCE, "2"))
        assertEquals(ExecutionMode.Loop, resolveExecutionMode(ExecutionModeSelection.LOOP, "2"))
        assertEquals(ExecutionMode.PingPong, resolveExecutionMode(ExecutionModeSelection.PING_PONG, "2"))
        assertEquals(ExecutionMode.Times(4), resolveExecutionMode(ExecutionModeSelection.TIMES, "4"))
    }

    @Test
    fun `the times mode needs a whole number above zero`() {
        assertNull(resolveExecutionMode(ExecutionModeSelection.TIMES, "0"))
        assertNull(resolveExecutionMode(ExecutionModeSelection.TIMES, "-1"))
        assertNull(resolveExecutionMode(ExecutionModeSelection.TIMES, "abc"))
        assertNull(resolveExecutionMode(ExecutionModeSelection.TIMES, ""))
    }
}
