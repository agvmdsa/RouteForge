package com.routeforge.simulation.presentation

import com.routeforge.coredomain.AddressLookup
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.handoff.LastComputedRouteHandoffImpl
import com.routeforge.coredomain.handoff.PendingTeleportTargetHandoffImpl
import com.routeforge.coredomain.model.AddressLabel
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.NamedPlace
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.testing.FakeMockLocationAuthorizationChecker
import com.routeforge.coredomain.testing.FakeNetworkConnectivityChecker
import com.routeforge.coredomain.testing.FakeRecentPlacesRepository
import com.routeforge.coredomain.testing.FakeReverseGeocoder
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
    private val lastComputedRoute = LastComputedRouteHandoffImpl()
    private val pendingTeleportTarget = PendingTeleportTargetHandoffImpl()
    private val recentPlaces = FakeRecentPlacesRepository()
    private val reverseGeocoder = FakeReverseGeocoder()
    private val confirmer =
        PendingTeleportConfirmer(
            teleport = TeleportUseCase(controller),
            isNetworkConnected = IsNetworkConnectedUseCase(networkConnectivityChecker),
            addressLookup = AddressLookup(reverseGeocoder),
            pendingTeleportTargetReceiver = pendingTeleportTarget,
            lastComputedRouteClearer = lastComputedRoute,
            recentPlacesRepository = recentPlaces,
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

        confirmer.registerMapTap(10.0, 20.0, state, scope)

        assertEquals(10.0, state.value.pendingTeleportLatitude)
        assertEquals(20.0, state.value.pendingTeleportLongitude)
        assertFalse(state.value.isPendingTeleportBlockedOffline)
        assertTrue(controller.teleportCalls.isEmpty())
    }

    @Test
    fun `tapping the map without a network marks the pending teleport as blocked offline`() {
        networkConnectivityChecker.connected = false
        val state = MutableStateFlow(SimulationState())

        confirmer.registerMapTap(10.0, 20.0, state, scope)

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

        pendingTeleportTarget.send(NamedPlace(latitude = 5.0, longitude = 6.0, name = "Home"))

        assertEquals(listOf(FakeSimulationController.TeleportCall(5.0, 6.0)), controller.teleportCalls)
        assertNull(state.value.pendingFavoriteTeleportTarget)
        assertNull(pendingTeleportTarget.pending)
    }

    @Test
    fun `a favorite target while unauthorized reports an error and never calls the controller`() {
        authorizationChecker.authorized = false
        val state = MutableStateFlow(SimulationState())
        confirmer.startObservingPendingTeleportTarget(state, scope)

        pendingTeleportTarget.send(NamedPlace(latitude = 5.0, longitude = 6.0, name = "Home"))

        assertTrue(controller.teleportCalls.isEmpty())
        assertEquals(SimulationErrorType.NOT_AUTHORIZED, state.value.errorType)
    }

    @Test
    fun `a favorite target while a route is loaded asks for confirmation instead of teleporting`() {
        val state = MutableStateFlow(SimulationState(loadedRoute = sampleRoute))
        confirmer.startObservingPendingTeleportTarget(state, scope)

        pendingTeleportTarget.send(NamedPlace(latitude = 5.0, longitude = 6.0, name = "Home"))

        assertEquals(NamedPlace(latitude = 5.0, longitude = 6.0, name = "Home"), state.value.pendingFavoriteTeleportTarget)
        assertTrue(controller.teleportCalls.isEmpty())
        assertNotNull(state.value.loadedRoute)
        assertNull(pendingTeleportTarget.pending)
    }

    @Test
    fun `confirming a favorite teleport cancels the loaded route and teleports`() {
        lastComputedRoute.set(sampleRoute)
        val state =
            MutableStateFlow(
                SimulationState(
                    loadedRoute = sampleRoute,
                    pendingFavoriteTeleportTarget = NamedPlace(latitude = 5.0, longitude = 6.0, name = "Home"),
                ),
            )

        confirmer.confirmFavoriteTeleportCancelRoute(state)

        assertNull(lastComputedRoute.route.value)
        assertEquals(listOf(FakeSimulationController.TeleportCall(5.0, 6.0)), controller.teleportCalls)
        assertNull(state.value.pendingFavoriteTeleportTarget)
    }

    @Test
    fun `confirming a favorite teleport when none is pending changes nothing`() {
        lastComputedRoute.set(sampleRoute)
        val state = MutableStateFlow(SimulationState(loadedRoute = sampleRoute))

        confirmer.confirmFavoriteTeleportCancelRoute(state)

        assertNotNull(lastComputedRoute.route.value)
        assertTrue(controller.teleportCalls.isEmpty())
    }

    @Test
    fun `a confirmed map tap teleport is recorded with no name`() {
        val state = MutableStateFlow(SimulationState(pendingTeleportLatitude = 10.0, pendingTeleportLongitude = 20.0))

        confirmer.confirmTeleport(state)

        assertEquals(listOf(NamedPlace(latitude = 10.0, longitude = 20.0, name = null)), recentPlaces.recorded)
    }

    @Test
    fun `a canceled map tap teleport is not recorded`() {
        val state = MutableStateFlow(SimulationState(pendingTeleportLatitude = 10.0, pendingTeleportLongitude = 20.0))

        confirmer.cancelTeleport(state)

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `a map tap teleport blocked offline is not recorded`() {
        val state =
            MutableStateFlow(
                SimulationState(pendingTeleportLatitude = 10.0, pendingTeleportLongitude = 20.0, isPendingTeleportBlockedOffline = true),
            )

        confirmer.confirmTeleport(state)

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `a map tap teleport while unauthorized is not recorded`() {
        authorizationChecker.authorized = false
        val state = MutableStateFlow(SimulationState(pendingTeleportLatitude = 10.0, pendingTeleportLongitude = 20.0))

        confirmer.confirmTeleport(state)

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `a named place teleported at once with no route loaded is recorded with its name`() {
        val state = MutableStateFlow(SimulationState())
        confirmer.startObservingPendingTeleportTarget(state, scope)

        pendingTeleportTarget.send(NamedPlace(latitude = 5.0, longitude = 6.0, name = "Home"))

        assertEquals(listOf(NamedPlace(latitude = 5.0, longitude = 6.0, name = "Home")), recentPlaces.recorded)
    }

    @Test
    fun `a place with no name teleported at once is recorded with no name`() {
        val state = MutableStateFlow(SimulationState())
        confirmer.startObservingPendingTeleportTarget(state, scope)

        pendingTeleportTarget.send(NamedPlace(latitude = 5.0, longitude = 6.0, name = null))

        assertEquals(listOf(NamedPlace(latitude = 5.0, longitude = 6.0, name = null)), recentPlaces.recorded)
    }

    @Test
    fun `a place handed over while unauthorized is not recorded`() {
        authorizationChecker.authorized = false
        val state = MutableStateFlow(SimulationState())
        confirmer.startObservingPendingTeleportTarget(state, scope)

        pendingTeleportTarget.send(NamedPlace(latitude = 5.0, longitude = 6.0, name = "Home"))

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `a place handed over while a route is loaded is recorded only after the cancel-route confirmation`() {
        val state = MutableStateFlow(SimulationState(loadedRoute = sampleRoute))
        confirmer.startObservingPendingTeleportTarget(state, scope)
        pendingTeleportTarget.send(NamedPlace(latitude = 5.0, longitude = 6.0, name = "Home"))
        assertTrue(recentPlaces.recorded.isEmpty())

        confirmer.confirmFavoriteTeleportCancelRoute(state)

        assertEquals(listOf(NamedPlace(latitude = 5.0, longitude = 6.0, name = "Home")), recentPlaces.recorded)
    }

    private val address = AddressLabel(full = "Pariser Platz 1, Mitte, Berlin", short = "Pariser Platz 1")

    private fun passTheLookupPause() {
        dispatcher.scheduler.advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS)
        dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `a map tap shows the address of the spot after the pause`() {
        reverseGeocoder.answer = Result.Success(address)
        val state = MutableStateFlow(SimulationState())

        confirmer.registerMapTap(52.5, 13.4, state, scope)
        assertNull(state.value.pendingTeleportAddress)
        passTheLookupPause()

        assertEquals(address, state.value.pendingTeleportAddress)
        assertEquals(listOf(52.5 to 13.4), reverseGeocoder.requests)
    }

    @Test
    fun `a map tap blocked offline makes no request and keeps the offline block`() {
        networkConnectivityChecker.connected = false
        reverseGeocoder.answer = Result.Success(address)
        val state = MutableStateFlow(SimulationState())

        confirmer.registerMapTap(52.5, 13.4, state, scope)
        passTheLookupPause()

        assertTrue(reverseGeocoder.requests.isEmpty())
        assertNull(state.value.pendingTeleportAddress)
        assertTrue(state.value.isPendingTeleportBlockedOffline)
    }

    @Test
    fun `a failed lookup leaves no address and confirming still teleports at once`() {
        reverseGeocoder.answer = Result.Error(DataError.Network.NO_INTERNET)
        val state = MutableStateFlow(SimulationState())
        confirmer.registerMapTap(10.0, 20.0, state, scope)
        passTheLookupPause()

        confirmer.confirmTeleport(state)

        assertEquals(listOf(FakeSimulationController.TeleportCall(10.0, 20.0)), controller.teleportCalls)
        assertNull(state.value.pendingTeleportAddress)
    }

    @Test
    fun `an answer of nothing found leaves no address`() {
        reverseGeocoder.answer = Result.Success(null)
        val state = MutableStateFlow(SimulationState())

        confirmer.registerMapTap(0.0, -30.0, state, scope)
        passTheLookupPause()

        assertEquals(1, reverseGeocoder.requests.size)
        assertNull(state.value.pendingTeleportAddress)
    }

    @Test
    fun `confirming before the address arrives discards it`() {
        reverseGeocoder.answer = Result.Success(address)
        reverseGeocoder.hold()
        val state = MutableStateFlow(SimulationState())
        confirmer.registerMapTap(10.0, 20.0, state, scope)
        passTheLookupPause()

        confirmer.confirmTeleport(state)
        reverseGeocoder.release()
        dispatcher.scheduler.runCurrent()

        assertNull(state.value.pendingTeleportAddress)
    }

    @Test
    fun `canceling before the address arrives discards it`() {
        reverseGeocoder.answer = Result.Success(address)
        reverseGeocoder.hold()
        val state = MutableStateFlow(SimulationState())
        confirmer.registerMapTap(10.0, 20.0, state, scope)
        passTheLookupPause()

        confirmer.cancelTeleport(state)
        reverseGeocoder.release()
        dispatcher.scheduler.runCurrent()

        assertNull(state.value.pendingTeleportAddress)
    }

    @Test
    fun `a new map tap clears the earlier address until its own arrives`() {
        reverseGeocoder.answer = Result.Success(address)
        val state = MutableStateFlow(SimulationState())
        confirmer.registerMapTap(10.0, 20.0, state, scope)
        passTheLookupPause()
        assertEquals(address, state.value.pendingTeleportAddress)

        confirmer.registerMapTap(30.0, 40.0, state, scope)

        assertNull(state.value.pendingTeleportAddress)
    }

    @Test
    fun `two map taps within the pause make one request, for the second position`() {
        reverseGeocoder.answer = Result.Success(address)
        val state = MutableStateFlow(SimulationState())

        confirmer.registerMapTap(10.0, 20.0, state, scope)
        dispatcher.scheduler.advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS / 2)
        confirmer.registerMapTap(30.0, 40.0, state, scope)
        passTheLookupPause()
        dispatcher.scheduler.advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS)
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(30.0 to 40.0), reverseGeocoder.requests)
        assertEquals(address, state.value.pendingTeleportAddress)
    }

    @Test
    fun `an answer held for the first tap never appears after a second tap`() {
        reverseGeocoder.answer = Result.Success(address)
        reverseGeocoder.hold()
        val state = MutableStateFlow(SimulationState())
        confirmer.registerMapTap(10.0, 20.0, state, scope)
        passTheLookupPause()

        confirmer.registerMapTap(30.0, 40.0, state, scope)
        reverseGeocoder.release()
        dispatcher.scheduler.runCurrent()

        assertNull(state.value.pendingTeleportAddress)
    }
}
