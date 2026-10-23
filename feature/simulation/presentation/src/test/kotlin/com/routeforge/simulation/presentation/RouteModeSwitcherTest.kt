package com.routeforge.simulation.presentation

import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.simulation.domain.usecase.UpdateActiveRouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RouteModeSwitcherTest {
    private val controller = FakeSimulationController()
    private val lastComputedRouteHolder = LastComputedRouteHolder()
    private val switcher =
        RouteModeSwitcher(
            updateActiveRouteUseCase = UpdateActiveRouteUseCase(controller),
            lastComputedRouteHolder = lastComputedRouteHolder,
        )

    private val guidedGeometry = listOf(LatLng(0.0, 0.0), LatLng(1.0, 1.0))
    private val freeRoamGeometry = listOf(LatLng(2.0, 2.0), LatLng(3.0, 3.0))

    private val routeWithAlternate =
        Route(
            points = emptyList(),
            geometry = guidedGeometry,
            distanceMeters = 100.0,
            mode = RoutePlaybackMode.GUIDED,
            alternateGeometry = freeRoamGeometry,
            alternateDistanceMeters = 50.0,
            waypointCumulativeDistances = listOf(0.0, 100.0),
        )

    private val routeWithoutAlternate = routeWithAlternate.copy(alternateGeometry = null, alternateDistanceMeters = null)

    @Test
    fun `clicking switch on a guided route proposes free roam`() {
        val state = MutableStateFlow(SimulationState(loadedRoute = routeWithAlternate))

        switcher.onSwitchRouteModeClick(state)

        assertEquals(RoutePlaybackMode.FREE_ROAM, state.value.pendingRouteModeSwitch)
    }

    @Test
    fun `clicking switch on a free roam route proposes guided`() {
        val freeRoam = routeWithAlternate.copy(mode = RoutePlaybackMode.FREE_ROAM)
        val state = MutableStateFlow(SimulationState(loadedRoute = freeRoam))

        switcher.onSwitchRouteModeClick(state)

        assertEquals(RoutePlaybackMode.GUIDED, state.value.pendingRouteModeSwitch)
    }

    @Test
    fun `clicking switch on a route with no alternate changes nothing`() {
        val state = MutableStateFlow(SimulationState(loadedRoute = routeWithoutAlternate))

        switcher.onSwitchRouteModeClick(state)

        assertNull(state.value.pendingRouteModeSwitch)
    }

    @Test
    fun `clicking switch on a route with an alternate geometry but no alternate distance changes nothing`() {
        val state = MutableStateFlow(SimulationState(loadedRoute = routeWithAlternate.copy(alternateDistanceMeters = null)))

        switcher.onSwitchRouteModeClick(state)

        assertNull(state.value.pendingRouteModeSwitch)
    }

    @Test
    fun `clicking switch with no route loaded changes nothing`() {
        val state = MutableStateFlow(SimulationState())

        switcher.onSwitchRouteModeClick(state)

        assertNull(state.value.pendingRouteModeSwitch)
    }

    @Test
    fun `confirming swaps geometry and mode, sets the holder and updates the active route`() {
        val state =
            MutableStateFlow(SimulationState(loadedRoute = routeWithAlternate, pendingRouteModeSwitch = RoutePlaybackMode.FREE_ROAM))

        switcher.confirmSwitchRouteMode(state)

        val swapped = lastComputedRouteHolder.route.value
        assertEquals(RoutePlaybackMode.FREE_ROAM, swapped?.mode)
        assertEquals(freeRoamGeometry, swapped?.geometry)
        assertEquals(50.0, swapped?.distanceMeters)
        assertEquals(guidedGeometry, swapped?.alternateGeometry)
        assertEquals(100.0, swapped?.alternateDistanceMeters)
        assertTrue(swapped?.waypointCumulativeDistances?.isEmpty() == true)
        assertEquals(listOf(swapped), controller.updateActiveRouteCalls)
        assertNull(state.value.pendingRouteModeSwitch)
    }

    @Test
    fun `confirming with nothing pending changes nothing`() {
        val state = MutableStateFlow(SimulationState(loadedRoute = routeWithAlternate))

        switcher.confirmSwitchRouteMode(state)

        assertNull(lastComputedRouteHolder.route.value)
        assertTrue(controller.updateActiveRouteCalls.isEmpty())
    }

    @Test
    fun `confirming clears the pending switch even when no route is loaded`() {
        val state = MutableStateFlow(SimulationState(pendingRouteModeSwitch = RoutePlaybackMode.FREE_ROAM))

        switcher.confirmSwitchRouteMode(state)

        assertNull(state.value.pendingRouteModeSwitch)
        assertNull(lastComputedRouteHolder.route.value)
        assertTrue(controller.updateActiveRouteCalls.isEmpty())
    }

    @Test
    fun `confirming on a route that lost its alternate swaps nothing`() {
        val state =
            MutableStateFlow(SimulationState(loadedRoute = routeWithoutAlternate, pendingRouteModeSwitch = RoutePlaybackMode.FREE_ROAM))

        switcher.confirmSwitchRouteMode(state)

        assertNull(state.value.pendingRouteModeSwitch)
        assertNull(lastComputedRouteHolder.route.value)
        assertTrue(controller.updateActiveRouteCalls.isEmpty())
    }

    @Test
    fun `applying an edited route sets the holder and updates the active route`() {
        val edited = routeWithAlternate.copy(distanceMeters = 999.0)

        switcher.applyEditedRoute(edited)

        assertEquals(edited, lastComputedRouteHolder.route.value)
        assertEquals(listOf(edited), controller.updateActiveRouteCalls)
    }
}
