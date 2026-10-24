package com.routeforge.simulation.presentation

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.JoystickStartFailure
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SimulationStatus
import com.routeforge.simulation.domain.model.SpeedSetting
import com.routeforge.simulation.domain.testing.FakeSimulationController
import com.routeforge.simulation.domain.usecase.ConfirmJoystickInterruptUseCase
import com.routeforge.simulation.domain.usecase.PauseSimulationUseCase
import com.routeforge.simulation.domain.usecase.RequestJoystickInterruptUseCase
import com.routeforge.simulation.domain.usecase.ResumeSimulationUseCase
import com.routeforge.simulation.domain.usecase.SetSpeedUseCase
import com.routeforge.simulation.domain.usecase.StopSimulationUseCase
import com.routeforge.simulation.domain.usecase.UpdateJoystickDirectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JoystickSessionControllerTest {
    private val controller = FakeSimulationController()
    private val lastComputedRouteHolder = LastComputedRouteHolder()
    private val joystickSessionController =
        JoystickSessionController(
            requestJoystickInterrupt = RequestJoystickInterruptUseCase(controller),
            confirmInterrupt = ConfirmJoystickInterruptUseCase(controller),
            updateJoystickDirection = UpdateJoystickDirectionUseCase(controller),
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

    private fun session(
        mode: SimulationMode,
        status: SimulationStatus = SimulationStatus.RUNNING,
    ) = SimulationSession(
        mode = mode,
        status = status,
        latitude = 10.0,
        longitude = 20.0,
        bearingDegrees = 0f,
        speedMetersPerSecond = 0f,
        route = null,
        distanceTraveledMeters = 0.0,
    )

    @Test
    fun `a drag with no real fix yet gives the no real fix error and never updates the direction`() {
        controller.startJoystickResult = Result.Error(JoystickStartFailure.NoRealLocationFixYet)
        val state = MutableStateFlow(SimulationState(isJoystickVisible = true))

        joystickSessionController.steerJoystick(0f, state)

        assertEquals(SimulationErrorType.JOYSTICK_NO_REAL_FIX, state.value.errorType)
        assertTrue(controller.updateJoystickDirectionCalls.isEmpty())
    }

    @Test
    fun `a drag after a failed start tries to start the session again`() {
        controller.startJoystickResult = Result.Error(JoystickStartFailure.NoRealLocationFixYet)
        val state = MutableStateFlow(SimulationState(isJoystickVisible = true))
        joystickSessionController.steerJoystick(0f, state)
        controller.startJoystickResult = Result.Success(Unit)

        joystickSessionController.steerJoystick(10f, state)

        assertEquals(2, controller.startJoystickCallCount)
        assertNull(state.value.errorType)
        assertEquals(listOf(10f), controller.updateJoystickDirectionCalls)
    }

    @Test
    fun `the first drag starts the session once and later frames only update the direction`() {
        val state = MutableStateFlow(SimulationState(isJoystickVisible = true))

        joystickSessionController.steerJoystick(10f, state)
        joystickSessionController.steerJoystick(15f, state)
        joystickSessionController.steerJoystick(20f, state)

        assertEquals(1, controller.startJoystickCallCount)
        assertEquals(listOf(10f, 15f, 20f), controller.updateJoystickDirectionCalls)
    }

    @Test
    fun `the first drag clears an earlier error and applies the current speed slider value`() {
        val state =
            MutableStateFlow(
                SimulationState(isJoystickVisible = true, joystickSpeedKmh = 18f, errorType = SimulationErrorType.JOYSTICK_NO_REAL_FIX),
            )

        joystickSessionController.steerJoystick(0f, state)

        assertNull(state.value.errorType)
        assertEquals(listOf(SpeedSetting.Manual(5f)), controller.setSpeedCalls)
    }

    @Test
    fun `a drag after a release resumes the paused session instead of leaving it stuck`() {
        val state = MutableStateFlow(SimulationState(isJoystickVisible = true))
        joystickSessionController.steerJoystick(45f, state)
        state.value = state.value.copy(mockedSession = session(SimulationMode.JOYSTICK, SimulationStatus.PAUSED))

        joystickSessionController.steerJoystick(90f, state)

        assertEquals(1, controller.resumeCallCount)
        assertEquals(1, controller.startJoystickCallCount)
        assertEquals(listOf(45f, 90f), controller.updateJoystickDirectionCalls)
    }

    @Test
    fun `a drag on a running session does not resume it`() {
        val state = MutableStateFlow(SimulationState(isJoystickVisible = true))
        joystickSessionController.steerJoystick(45f, state)
        state.value = state.value.copy(mockedSession = session(SimulationMode.JOYSTICK))

        joystickSessionController.steerJoystick(90f, state)

        assertEquals(0, controller.resumeCallCount)
    }

    @Test
    fun `releasing the joystick pauses the session in place`() {
        joystickSessionController.pauseJoystickSession()

        assertEquals(1, controller.pauseCallCount)
    }

    @Test
    fun `the session is started again after the mocked session stops being a joystick one`() {
        val state = MutableStateFlow(SimulationState(isJoystickVisible = true))
        joystickSessionController.steerJoystick(10f, state)

        joystickSessionController.onMockedSessionChanged(null)
        joystickSessionController.steerJoystick(20f, state)

        assertEquals(2, controller.startJoystickCallCount)
    }

    @Test
    fun `the session is started again after a route session replaces the joystick one`() {
        val state = MutableStateFlow(SimulationState(isJoystickVisible = true))
        joystickSessionController.steerJoystick(10f, state)

        joystickSessionController.onMockedSessionChanged(session(SimulationMode.ROUTE))
        joystickSessionController.steerJoystick(20f, state)

        assertEquals(2, controller.startJoystickCallCount)
    }

    @Test
    fun `the session is not started again while the mocked session is still a joystick one`() {
        val state = MutableStateFlow(SimulationState(isJoystickVisible = true))
        joystickSessionController.steerJoystick(10f, state)

        joystickSessionController.onMockedSessionChanged(session(SimulationMode.JOYSTICK))
        joystickSessionController.steerJoystick(20f, state)

        assertEquals(1, controller.startJoystickCallCount)
    }

    @Test
    fun `toggling the joystick on with nothing loaded or running reveals it at once`() {
        val state = MutableStateFlow(SimulationState())

        joystickSessionController.toggleJoystick(state)

        assertTrue(state.value.isJoystickVisible)
        assertFalse(state.value.isJoystickInterruptPending)
    }

    @Test
    fun `toggling the joystick on while a route is loaded asks first`() {
        val state = MutableStateFlow(SimulationState(loadedRoute = sampleRoute))

        joystickSessionController.toggleJoystick(state)

        assertTrue(state.value.isJoystickInterruptPending)
        assertFalse(state.value.isJoystickVisible)
    }

    @Test
    fun `toggling the joystick on while a route is playing asks first`() {
        val state = MutableStateFlow(SimulationState())
        controller.emit(session(SimulationMode.ROUTE))

        joystickSessionController.toggleJoystick(state)

        assertTrue(state.value.isJoystickInterruptPending)
        assertFalse(state.value.isJoystickVisible)
    }

    @Test
    fun `toggling the joystick off while it is mocking stops the session`() {
        val state =
            MutableStateFlow(SimulationState(isJoystickVisible = true, mockedSession = session(SimulationMode.JOYSTICK)))

        joystickSessionController.toggleJoystick(state)

        assertEquals(1, controller.stopCallCount)
        assertFalse(state.value.isJoystickVisible)
    }

    @Test
    fun `toggling the joystick off before it was ever dragged stops nothing`() {
        val state = MutableStateFlow(SimulationState(isJoystickVisible = true))

        joystickSessionController.toggleJoystick(state)

        assertEquals(0, controller.stopCallCount)
        assertFalse(state.value.isJoystickVisible)
    }

    @Test
    fun `toggling the joystick off lets the next drag start a new session`() {
        val state = MutableStateFlow(SimulationState(isJoystickVisible = true))
        joystickSessionController.steerJoystick(10f, state)
        joystickSessionController.toggleJoystick(state)
        joystickSessionController.toggleJoystick(state)

        joystickSessionController.steerJoystick(20f, state)

        assertEquals(2, controller.startJoystickCallCount)
    }

    @Test
    fun `changing the joystick speed updates the slider and reaches the controller only while a session is active`() {
        val idle = MutableStateFlow(SimulationState())
        val active = MutableStateFlow(SimulationState(mockedSession = session(SimulationMode.JOYSTICK)))

        joystickSessionController.changeJoystickSpeed(18f, idle)
        assertEquals(18f, idle.value.joystickSpeedKmh)
        assertTrue(controller.setSpeedCalls.isEmpty())

        joystickSessionController.changeJoystickSpeed(36f, active)
        assertEquals(36f, active.value.joystickSpeedKmh)
        assertEquals(listOf(SpeedSetting.Manual(10f)), controller.setSpeedCalls)
    }

    @Test
    fun `confirming the interrupt clears the route, stops the session and reveals the joystick`() {
        lastComputedRouteHolder.set(sampleRoute)
        val state = MutableStateFlow(SimulationState(loadedRoute = sampleRoute, isJoystickInterruptPending = true))

        joystickSessionController.confirmJoystickInterrupt(state)

        assertNull(lastComputedRouteHolder.route.value)
        assertEquals(1, controller.stopCallCount)
        assertFalse(state.value.isJoystickInterruptPending)
        assertTrue(state.value.isJoystickVisible)
    }

    @Test
    fun `confirming the interrupt lets the next drag start a new session`() {
        val state = MutableStateFlow(SimulationState(isJoystickVisible = true))
        joystickSessionController.steerJoystick(10f, state)

        joystickSessionController.confirmJoystickInterrupt(state)
        joystickSessionController.steerJoystick(20f, state)

        assertEquals(2, controller.startJoystickCallCount)
    }
}
