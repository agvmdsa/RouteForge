package com.routeforge.simulation.presentation

import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.usecase.ObserveSimulationSessionUseCase
import com.routeforge.simulation.domain.usecase.PauseSimulationUseCase
import com.routeforge.simulation.domain.usecase.ResumeSimulationUseCase
import com.routeforge.simulation.domain.usecase.StartRouteSimulationUseCase
import com.routeforge.simulation.domain.usecase.StopSimulationUseCase
import com.routeforge.simulation.domain.usecase.TeleportUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val sampleRoute =
    Route(
        points = emptyList(),
        geometry = listOf(0.0 to 0.0, 1.0 to 1.0),
        distanceMeters = 100.0,
    )

class SimulationViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val controller = FakeSimulationController()
    private val authorizationChecker = FakeMockLocationAuthorizationChecker()
    private val lastComputedRouteHolder = LastComputedRouteHolder()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SimulationViewModel =
        SimulationViewModel(
            teleportUseCase = TeleportUseCase(controller),
            startRouteSimulationUseCase = StartRouteSimulationUseCase(controller),
            pauseSimulationUseCase = PauseSimulationUseCase(controller),
            resumeSimulationUseCase = ResumeSimulationUseCase(controller),
            stopSimulationUseCase = StopSimulationUseCase(controller),
            observeSimulationSessionUseCase = ObserveSimulationSessionUseCase(controller),
            mockLocationAuthorizationChecker = authorizationChecker,
            lastComputedRouteHolder = lastComputedRouteHolder,
        )

    @Test
    fun `tapping the map sets a pending teleport point without calling the controller`() {
        val viewModel = createViewModel()

        viewModel.onAction(SimulationAction.OnMapTap(latitude = 10.0, longitude = 20.0))

        assertEquals(10.0, viewModel.state.value.pendingTeleportLatitude)
        assertEquals(20.0, viewModel.state.value.pendingTeleportLongitude)
        assertTrue(controller.teleportCalls.isEmpty())
    }

    @Test
    fun `confirming a pending teleport calls the controller and clears the pending point`() {
        val viewModel = createViewModel()
        viewModel.onAction(SimulationAction.OnMapTap(latitude = 10.0, longitude = 20.0))

        viewModel.onAction(SimulationAction.OnConfirmTeleport)

        assertEquals(1, controller.teleportCalls.size)
        assertEquals(10.0, controller.teleportCalls.first().latitude)
        assertEquals(20.0, controller.teleportCalls.first().longitude)
        assertNull(viewModel.state.value.pendingTeleportLatitude)
        assertNull(viewModel.state.value.pendingTeleportLongitude)
    }

    @Test
    fun `canceling a pending teleport clears it without calling the controller`() {
        val viewModel = createViewModel()
        viewModel.onAction(SimulationAction.OnMapTap(latitude = 10.0, longitude = 20.0))

        viewModel.onAction(SimulationAction.OnCancelTeleport)

        assertTrue(controller.teleportCalls.isEmpty())
        assertNull(viewModel.state.value.pendingTeleportLatitude)
        assertNull(viewModel.state.value.pendingTeleportLongitude)
    }

    @Test
    fun `confirming a teleport while unauthorized reports an error and never calls the controller`() {
        authorizationChecker.authorized = false
        val viewModel = createViewModel()
        viewModel.onAction(SimulationAction.OnMapTap(latitude = 10.0, longitude = 20.0))

        viewModel.onAction(SimulationAction.OnConfirmTeleport)

        assertTrue(controller.teleportCalls.isEmpty())
        assertTrue(viewModel.state.value.isBlockedByAuthorization)
        assertEquals(SimulationErrorType.NOT_AUTHORIZED, viewModel.state.value.errorType)
    }

    @Test
    fun `starting a route simulation while unauthorized reports a distinct error and never calls the controller`() {
        authorizationChecker.authorized = false
        lastComputedRouteHolder.set(sampleRoute)
        val viewModel = createViewModel()
        viewModel.onAction(SimulationAction.OnSpeedInputChange("5.0"))

        viewModel.onAction(SimulationAction.OnStartRouteSimulation)

        assertTrue(controller.startRouteCalls.isEmpty())
        assertTrue(viewModel.state.value.isBlockedByAuthorization)
        assertEquals(SimulationErrorType.NOT_AUTHORIZED, viewModel.state.value.errorType)
    }

    @Test
    fun `starting a route simulation with a loaded route and valid speed calls the controller`() {
        lastComputedRouteHolder.set(sampleRoute)
        val viewModel = createViewModel()
        viewModel.onAction(SimulationAction.OnSpeedInputChange("5.0"))

        viewModel.onAction(SimulationAction.OnStartRouteSimulation)

        assertEquals(1, controller.startRouteCalls.size)
        assertEquals(sampleRoute, controller.startRouteCalls.first().route)
        assertEquals(5.0f, controller.startRouteCalls.first().speedMetersPerSecond)
    }

    @Test
    fun `pause resume and stop actions delegate directly to the controller`() {
        val viewModel = createViewModel()

        viewModel.onAction(SimulationAction.OnPauseSimulation)
        viewModel.onAction(SimulationAction.OnResumeSimulation)
        viewModel.onAction(SimulationAction.OnStopSimulation)

        assertEquals(1, controller.pauseCallCount)
        assertEquals(1, controller.resumeCallCount)
        assertEquals(1, controller.stopCallCount)
    }
}
