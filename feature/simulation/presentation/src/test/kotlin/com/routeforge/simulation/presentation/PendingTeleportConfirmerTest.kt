package com.routeforge.simulation.presentation

import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.holder.PendingTeleportTargetHolder
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.testing.FakeMockLocationAuthorizationChecker
import com.routeforge.coredomain.testing.FakeNetworkConnectivityChecker
import com.routeforge.simulation.domain.testing.FakeSimulationController
import com.routeforge.simulation.domain.usecase.IsNetworkConnectedUseCase
import com.routeforge.simulation.domain.usecase.ObserveAuthorizationLostUseCase
import com.routeforge.simulation.domain.usecase.TeleportUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PendingTeleportConfirmerTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val controller = FakeSimulationController()
    private val authorizationChecker = FakeMockLocationAuthorizationChecker()
    private val networkConnectivityChecker = FakeNetworkConnectivityChecker()
    private val lastComputedRouteHolder = LastComputedRouteHolder()
    private val pendingTeleportTargetHolder = PendingTeleportTargetHolder()
    private val confirmer =
        PendingTeleportConfirmer(
            teleportUseCase = TeleportUseCase(controller),
            isNetworkConnectedUseCase = IsNetworkConnectedUseCase(networkConnectivityChecker),
            pendingTeleportTargetHolder = pendingTeleportTargetHolder,
            lastComputedRouteHolder = lastComputedRouteHolder,
            mockLocationAuthorizationGuard =
                MockLocationAuthorizationGuard(authorizationChecker, ObserveAuthorizationLostUseCase(controller)),
        )

    private val sampleRoute =
        Route(
            points = emptyList(),
            geometry = listOf(LatLng(0.0, 0.0), LatLng(1.0, 1.0)),
            distanceMeters = 100.0,
        )

    @Test
    fun `tapping the map sets a pending teleport point without calling the controller`() {
        val state = MutableStateFlow(SimulationState())

        confirmer.registerMapTap(10.0, 20.0, state)

        assertEquals(10.0, state.value.pendingTeleportLatitude)
        assertEquals(20.0, state.value.pendingTeleportLongitude)
        assertFalse(state.value.isPendingTeleportBlockedOffline)
        assertTrue(controller.teleportCalls.isEmpty())
    }

    @Test
    fun `tapping the map without a network marks the pending teleport as blocked offline`() {
        networkConnectivityChecker.connected = false
        val state = MutableStateFlow(SimulationState())

        confirmer.registerMapTap(10.0, 20.0, state)

        assertTrue(state.value.isPendingTeleportBlockedOffline)
    }

    @Test
    fun `confirming a pending teleport calls the controller and clears the pending point`() {
        val state = MutableStateFlow(SimulationState(pendingTeleportLatitude = 10.0, pendingTeleportLongitude = 20.0))

        confirmer.confirmTeleport(state)

        assertEquals(listOf(FakeSimulationController.TeleportCall(10.0, 20.0)), controller.teleportCalls)
        assertNull(state.value.pendingTeleportLatitude)
        assertNull(state.value.pendingTeleportLongitude)
    }

    @Test
    fun `confirming a teleport that is blocked offline clears the pending point and never calls the controller`() {
        val state =
            MutableStateFlow(
                SimulationState(pendingTeleportLatitude = 10.0, pendingTeleportLongitude = 20.0, isPendingTeleportBlockedOffline = true),
            )

        confirmer.confirmTeleport(state)

        assertTrue(controller.teleportCalls.isEmpty())
        assertNull(state.value.pendingTeleportLatitude)
        assertFalse(state.value.isPendingTeleportBlockedOffline)
    }

    @Test
    fun `confirming a teleport while unauthorized reports an error and never calls the controller`() {
        authorizationChecker.authorized = false
        val state = MutableStateFlow(SimulationState(pendingTeleportLatitude = 10.0, pendingTeleportLongitude = 20.0))

        confirmer.confirmTeleport(state)

        assertTrue(controller.teleportCalls.isEmpty())
        assertEquals(SimulationErrorType.NOT_AUTHORIZED, state.value.errorType)
        assertTrue(state.value.isBlockedByAuthorization)
    }

    @Test
    fun `canceling a pending teleport clears it without calling the controller`() {
        val state =
            MutableStateFlow(
                SimulationState(pendingTeleportLatitude = 10.0, pendingTeleportLongitude = 20.0, isPendingTeleportBlockedOffline = true),
            )

        confirmer.cancelTeleport(state)

        assertTrue(controller.teleportCalls.isEmpty())
        assertNull(state.value.pendingTeleportLatitude)
        assertNull(state.value.pendingTeleportLongitude)
        assertFalse(state.value.isPendingTeleportBlockedOffline)
    }

    @Test
    fun `a favorite target with no route loaded teleports at once and the holder is cleared`() {
        val state = MutableStateFlow(SimulationState())
        confirmer.startObservingPendingTeleportTarget(state, scope)

        pendingTeleportTargetHolder.set(RoutePoint(latitude = 5.0, longitude = 6.0))

        assertEquals(listOf(FakeSimulationController.TeleportCall(5.0, 6.0)), controller.teleportCalls)
        assertNull(state.value.pendingFavoriteTeleportTarget)
        assertNull(pendingTeleportTargetHolder.target.value)
    }

    @Test
    fun `a favorite target while unauthorized reports an error and never calls the controller`() {
        authorizationChecker.authorized = false
        val state = MutableStateFlow(SimulationState())
        confirmer.startObservingPendingTeleportTarget(state, scope)

        pendingTeleportTargetHolder.set(RoutePoint(latitude = 5.0, longitude = 6.0))

        assertTrue(controller.teleportCalls.isEmpty())
        assertEquals(SimulationErrorType.NOT_AUTHORIZED, state.value.errorType)
    }

    @Test
    fun `a favorite target while a route is loaded asks for confirmation instead of teleporting`() {
        val state = MutableStateFlow(SimulationState(loadedRoute = sampleRoute))
        confirmer.startObservingPendingTeleportTarget(state, scope)

        pendingTeleportTargetHolder.set(RoutePoint(latitude = 5.0, longitude = 6.0))

        assertEquals(RoutePoint(latitude = 5.0, longitude = 6.0), state.value.pendingFavoriteTeleportTarget)
        assertTrue(controller.teleportCalls.isEmpty())
        assertNotNull(state.value.loadedRoute)
        assertNull(pendingTeleportTargetHolder.target.value)
    }

    @Test
    fun `confirming a favorite teleport cancels the loaded route and teleports`() {
        lastComputedRouteHolder.set(sampleRoute)
        val state =
            MutableStateFlow(
                SimulationState(loadedRoute = sampleRoute, pendingFavoriteTeleportTarget = RoutePoint(latitude = 5.0, longitude = 6.0)),
            )

        confirmer.confirmFavoriteTeleportCancelRoute(state)

        assertNull(lastComputedRouteHolder.route.value)
        assertEquals(listOf(FakeSimulationController.TeleportCall(5.0, 6.0)), controller.teleportCalls)
        assertNull(state.value.pendingFavoriteTeleportTarget)
    }

    @Test
    fun `confirming a favorite teleport when none is pending changes nothing`() {
        lastComputedRouteHolder.set(sampleRoute)
        val state = MutableStateFlow(SimulationState(loadedRoute = sampleRoute))

        confirmer.confirmFavoriteTeleportCancelRoute(state)

        assertNotNull(lastComputedRouteHolder.route.value)
        assertTrue(controller.teleportCalls.isEmpty())
    }
}
