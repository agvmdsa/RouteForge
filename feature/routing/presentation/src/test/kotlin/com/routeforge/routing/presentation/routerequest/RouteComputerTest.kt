package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.Region
import com.routeforge.coredomain.model.RegionStatus
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.testing.FakeRegionCatalog
import com.routeforge.coredomain.testing.FakeRegionUsageTracker
import com.routeforge.coredomain.testing.FakeRoutingEngine
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.coredomain.usecase.ComputeRouteUseCase
import com.routeforge.coredomain.usecase.FreeRoamRouteBuilder
import com.routeforge.coredomain.usecase.PrepareRouteOptionsUseCase
import com.routeforge.coredomain.usecase.RecordRegionUsageUseCase
import com.routeforge.routing.domain.model.RouteDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RouteComputerTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val routingEngine = FakeRoutingEngine(snappableLatitudes = emptySet())
    private val regionCatalog = FakeRegionCatalog()
    private val lastComputedRouteHolder = LastComputedRouteHolder()
    private val computer =
        RouteComputer(
            prepareRouteOptions =
                PrepareRouteOptionsUseCase(
                    computeRoute = ComputeRouteUseCase(routingEngine, regionCatalog, RecordRegionUsageUseCase(FakeRegionUsageTracker())),
                    freeRoamRouteBuilder = FreeRoamRouteBuilder(),
                ),
            computeRequiredRegions = ComputeRequiredRegionsUseCase(regionCatalog),
            lastComputedRouteHolder = lastComputedRouteHolder,
            backgroundDispatcher = dispatcher,
        )

    private val twoPoints = RouteDraft(points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)))

    private fun makeGuidedViable() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0)
        routingEngine.computePathResult =
            Route(
                points = twoPoints.points,
                geometry = listOf(LatLng(1.0, 1.0), LatLng(2.0, 2.0)),
                distanceMeters = 100.0,
            )
    }

    @Test
    fun `requesting a route with fewer than two points is blocked with a message`() {
        val state = MutableStateFlow(RouteRequestState(draft = RouteDraft(points = listOf(RoutePoint(1.0, 1.0)))))

        computer.requestRoute(state, scope)

        assertEquals(RouteRequestError.TOO_FEW_WAYPOINTS_TO_PLAY, state.value.errorType)
        assertNull(state.value.route)
        assertFalse(state.value.isComputing)
    }

    @Test
    fun `requesting a route while a region is missing sets the warning and does not compute`() {
        regionCatalog.regions =
            listOf(
                Region(
                    id = "north-zone",
                    displayName = "North Zone",
                    minLatitude = 0.0,
                    minLongitude = 0.0,
                    maxLatitude = 5.0,
                    maxLongitude = 5.0,
                    tileIds = listOf("north.rd5"),
                    approximateSizeBytes = 1_000L,
                    status = RegionStatus.NOT_DOWNLOADED,
                    missingTileCount = 1,
                ),
            )
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))

        computer.requestRoute(state, scope)

        assertNotNull(state.value.missingRegionsWarning)
        assertNull(state.value.route)
        assertFalse(state.value.isComputing)
        assertNull(lastComputedRouteHolder.route.value)
    }

    @Test
    fun `a guided route that cannot be computed resolves to free-roam with no alternate and no error`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))

        computer.requestRoute(state, scope)

        assertEquals(RoutePlaybackMode.FREE_ROAM, state.value.chosenMode)
        assertEquals(RoutePlaybackMode.FREE_ROAM, state.value.route?.mode)
        assertNull(state.value.route?.alternateGeometry)
        assertNull(state.value.errorType)
        assertEquals(state.value.route, lastComputedRouteHolder.route.value)
    }

    @Test
    fun `with both modes viable and no preview the route is free-roam and keeps the guided path as its alternate`() {
        makeGuidedViable()
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))

        computer.requestRoute(state, scope)

        assertEquals(RoutePlaybackMode.FREE_ROAM, state.value.route?.mode)
        assertNotNull(state.value.route?.alternateGeometry)
        assertNotNull(state.value.route?.alternateDistanceMeters)
        assertFalse(state.value.isComputing)
    }

    @Test
    fun `a guided preview carries through to a guided route that keeps the free-roam path as its alternate`() {
        makeGuidedViable()
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints, previewMode = RoutePlaybackMode.GUIDED))

        computer.requestRoute(state, scope)

        assertEquals(RoutePlaybackMode.GUIDED, state.value.route?.mode)
        assertEquals(RoutePlaybackMode.GUIDED, state.value.chosenMode)
        assertNotNull(state.value.route?.alternateGeometry)
        assertNull(state.value.previewMode)
        assertNull(state.value.previewRoute)
    }

    @Test
    fun `computing again replaces the previous route and clears the error`() {
        val previous = Route(points = emptyList(), geometry = listOf(LatLng(9.0, 9.0)), distanceMeters = 1.0)
        val state =
            MutableStateFlow(
                RouteRequestState(
                    draft = twoPoints,
                    route = previous,
                    chosenMode = RoutePlaybackMode.GUIDED,
                    errorType = RouteRequestError.MODE_PREVIEW_NO_PATH,
                ),
            )

        computer.proceedWithRouteComputation(twoPoints.points, state, scope)

        assertNull(state.value.errorType)
        assertEquals(RoutePlaybackMode.FREE_ROAM, state.value.chosenMode)
        assertEquals(twoPoints.points, state.value.route?.points)
    }
}
