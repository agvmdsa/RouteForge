package com.routeforge.simulation.presentation

import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.LastKnownRealLocationHolder
import com.routeforge.simulation.domain.model.RealLocation
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SimulationStatus
import com.routeforge.simulation.domain.usecase.IsNetworkConnectedUseCase
import com.routeforge.simulation.domain.usecase.ObserveMockedSessionUseCase
import com.routeforge.simulation.domain.usecase.ObserveRealLocationUseCase
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
    private val realLocationDataSource = FakeRealLocationDataSource()
    private val networkConnectivityChecker = FakeNetworkConnectivityChecker()
    private val lastKnownRealLocationHolder = LastKnownRealLocationHolder()

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
            observeMockedSessionUseCase = ObserveMockedSessionUseCase(controller),
            observeRealLocationUseCase = ObserveRealLocationUseCase(realLocationDataSource),
            isNetworkConnectedUseCase = IsNetworkConnectedUseCase(networkConnectivityChecker),
            mockLocationAuthorizationChecker = authorizationChecker,
            lastComputedRouteHolder = lastComputedRouteHolder,
            lastKnownRealLocationHolder = lastKnownRealLocationHolder,
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

    @Test
    fun `clicking cancel mock sets a pending confirmation without stopping the controller`() {
        val viewModel = createViewModel()

        viewModel.onAction(SimulationAction.OnCancelMockClick)

        assertTrue(viewModel.state.value.isPendingCancelMock)
        assertEquals(0, controller.stopCallCount)
    }

    @Test
    fun `confirming cancel mock stops the controller and clears the pending confirmation`() {
        val viewModel = createViewModel()
        viewModel.onAction(SimulationAction.OnCancelMockClick)

        viewModel.onAction(SimulationAction.OnConfirmCancelMock)

        assertEquals(1, controller.stopCallCount)
        assertTrue(!viewModel.state.value.isPendingCancelMock)
    }

    @Test
    fun `dismissing cancel mock clears the pending confirmation without stopping the controller`() {
        val viewModel = createViewModel()
        viewModel.onAction(SimulationAction.OnCancelMockClick)

        viewModel.onAction(SimulationAction.OnDismissCancelMock)

        assertEquals(0, controller.stopCallCount)
        assertTrue(!viewModel.state.value.isPendingCancelMock)
    }

    @Test
    fun `tapping the map while offline blocks the pending teleport from being confirmed`() {
        networkConnectivityChecker.connected = false
        val viewModel = createViewModel()

        viewModel.onAction(SimulationAction.OnMapTap(latitude = 10.0, longitude = 20.0))
        assertTrue(viewModel.state.value.isPendingTeleportBlockedOffline)

        viewModel.onAction(SimulationAction.OnConfirmTeleport)

        assertTrue(controller.teleportCalls.isEmpty())
    }

    @Test
    fun `tapping the map while online never blocks the pending teleport`() {
        networkConnectivityChecker.connected = true
        val viewModel = createViewModel()

        viewModel.onAction(SimulationAction.OnMapTap(latitude = 10.0, longitude = 20.0))

        assertTrue(!viewModel.state.value.isPendingTeleportBlockedOffline)
    }

    @Test
    fun `on startup with no active mock session the real location is searched and cached`() {
        realLocationDataSource.location = RealLocation(latitude = 1.0, longitude = 2.0)

        val viewModel = createViewModel()

        assertEquals(RealLocation(latitude = 1.0, longitude = 2.0), viewModel.state.value.realLocation)
        assertTrue(!viewModel.state.value.isSearchingRealLocation)
    }

    @Test
    fun `real location keeps updating continuously while no mock is active`() {
        val viewModel = createViewModel()
        realLocationDataSource.location = RealLocation(latitude = 1.0, longitude = 1.0)
        assertEquals(RealLocation(latitude = 1.0, longitude = 1.0), viewModel.state.value.realLocation)

        realLocationDataSource.location = RealLocation(latitude = 2.0, longitude = 2.0)

        assertEquals(RealLocation(latitude = 2.0, longitude = 2.0), viewModel.state.value.realLocation)
    }

    @Test
    fun `stopping an active mock session triggers a fresh real location search`() {
        realLocationDataSource.location = RealLocation(latitude = 1.0, longitude = 2.0)
        val viewModel = createViewModel()
        controller.emit(
            SimulationSession(
                mode = SimulationMode.STATIONARY,
                status = SimulationStatus.RUNNING,
                latitude = 10.0,
                longitude = 20.0,
                bearingDegrees = 0f,
                speedMetersPerSecond = 0f,
                route = null,
                distanceTraveledMeters = 0.0,
            ),
        )
        realLocationDataSource.location = RealLocation(latitude = 3.0, longitude = 4.0)

        controller.emit(null)

        assertEquals(RealLocation(latitude = 3.0, longitude = 4.0), viewModel.state.value.realLocation)
    }

    @Test
    fun `real location updates while a mock is active are ignored`() {
        val viewModel = createViewModel()
        controller.emit(
            SimulationSession(
                mode = SimulationMode.STATIONARY,
                status = SimulationStatus.RUNNING,
                latitude = 10.0,
                longitude = 20.0,
                bearingDegrees = 0f,
                speedMetersPerSecond = 0f,
                route = null,
                distanceTraveledMeters = 0.0,
            ),
        )

        realLocationDataSource.location = RealLocation(latitude = 9.0, longitude = 9.0)

        assertNull(viewModel.state.value.realLocation)
    }

    @Test
    fun `granting location permission after an initial failure restarts the real location observation`() {
        realLocationDataSource.permissionGranted = false
        val viewModel = createViewModel()
        realLocationDataSource.permissionGranted = true
        realLocationDataSource.location = RealLocation(latitude = 5.0, longitude = 6.0)

        viewModel.onAction(SimulationAction.OnLocationPermissionGranted)

        assertEquals(RealLocation(latitude = 5.0, longitude = 6.0), viewModel.state.value.realLocation)
    }

    @Test
    fun `granting location permission while a mock is active does not search the real location`() {
        val viewModel = createViewModel()
        controller.emit(
            SimulationSession(
                mode = SimulationMode.STATIONARY,
                status = SimulationStatus.RUNNING,
                latitude = 10.0,
                longitude = 20.0,
                bearingDegrees = 0f,
                speedMetersPerSecond = 0f,
                route = null,
                distanceTraveledMeters = 0.0,
            ),
        )
        realLocationDataSource.location = RealLocation(latitude = 5.0, longitude = 6.0)

        viewModel.onAction(SimulationAction.OnLocationPermissionGranted)

        assertNull(viewModel.state.value.realLocation)
    }
}
