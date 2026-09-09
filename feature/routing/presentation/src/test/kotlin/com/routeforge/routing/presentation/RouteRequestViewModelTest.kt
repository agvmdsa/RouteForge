package com.routeforge.routing.presentation

import com.routeforge.routing.domain.RoutingEngine
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import com.routeforge.routing.domain.model.Route
import com.routeforge.routing.domain.model.RoutePoint
import com.routeforge.routing.domain.usecase.ComputeRouteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class FakeRoutingEngine : RoutingEngine {
    var snappableLatitudes: Set<Double> = emptySet()
    var computePathResult: Route? = null

    override fun snap(
        point: RoutePoint,
        availableRegions: List<Region>,
    ): RoutePoint =
        if (point.latitude in snappableLatitudes) {
            point.copy(snappedLatitude = point.latitude, snappedLongitude = point.longitude)
        } else {
            point
        }

    override fun computePath(points: List<RoutePoint>): Route? = computePathResult
}

class RouteRequestViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val routingEngine = FakeRoutingEngine()
    private val regionCatalog = FakeRegionCatalog()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RouteRequestViewModel =
        RouteRequestViewModel(
            computeRoute = ComputeRouteUseCase(routingEngine, regionCatalog),
            backgroundDispatcher = dispatcher,
        )

    @Test
    fun `requesting a route with a valid start and end populates state with the resulting route`() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0)
        val expectedRoute =
            Route(
                points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)),
                geometry = listOf(1.0 to 1.0, 2.0 to 2.0),
                distanceMeters = 100.0,
            )
        routingEngine.computePathResult = expectedRoute
        val viewModel = createViewModel()

        viewModel.onAction(RouteRequestAction.OnStartLatitudeChange("1.0"))
        viewModel.onAction(RouteRequestAction.OnStartLongitudeChange("1.0"))
        viewModel.onAction(RouteRequestAction.OnEndLatitudeChange("2.0"))
        viewModel.onAction(RouteRequestAction.OnEndLongitudeChange("2.0"))
        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        assertEquals(expectedRoute, viewModel.state.value.route)
        assertNull(viewModel.state.value.errorMessage)
        assertTrue(!viewModel.state.value.isComputing)
    }

    @Test
    fun `adding and removing a waypoint updates state and is included in the next route request`() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0, 3.0)
        routingEngine.computePathResult =
            Route(points = emptyList(), geometry = emptyList(), distanceMeters = 0.0)
        val viewModel = createViewModel()

        viewModel.onAction(RouteRequestAction.OnAddWaypoint)
        assertEquals(1, viewModel.state.value.waypoints.size)

        viewModel.onAction(RouteRequestAction.OnWaypointLatitudeChange(0, "3.0"))
        viewModel.onAction(RouteRequestAction.OnWaypointLongitudeChange(0, "3.0"))
        val stateAfterEdit = viewModel.state.value
        assertEquals("3.0", stateAfterEdit.waypoints[0].latitudeInput)

        viewModel.onAction(RouteRequestAction.OnStartLatitudeChange("1.0"))
        viewModel.onAction(RouteRequestAction.OnStartLongitudeChange("1.0"))
        viewModel.onAction(RouteRequestAction.OnEndLatitudeChange("2.0"))
        viewModel.onAction(RouteRequestAction.OnEndLongitudeChange("2.0"))
        viewModel.onAction(RouteRequestAction.OnRequestRoute)
        assertNotNull(viewModel.state.value.route)

        viewModel.onAction(RouteRequestAction.OnRemoveWaypoint(0))
        val stateAfterRemoval = viewModel.state.value
        assertTrue(stateAfterRemoval.waypoints.isEmpty())
    }

    @Test
    fun `a point that cannot snap but is inside a known region reports a distinct not routable message`() {
        regionCatalog.regions =
            listOf(
                Region(
                    id = "r",
                    displayName = "R",
                    minLatitude = -10.0,
                    minLongitude = -10.0,
                    maxLatitude = 10.0,
                    maxLongitude = 10.0,
                    tileIds = listOf("t.rd5"),
                    approximateSizeBytes = 1,
                    status = RegionStatus.BUNDLED,
                ),
            )
        routingEngine.snappableLatitudes = setOf(2.0)
        val viewModel = createViewModel()

        viewModel.onAction(RouteRequestAction.OnStartLatitudeChange("1.0"))
        viewModel.onAction(RouteRequestAction.OnStartLongitudeChange("1.0"))
        viewModel.onAction(RouteRequestAction.OnEndLatitudeChange("2.0"))
        viewModel.onAction(RouteRequestAction.OnEndLongitudeChange("2.0"))
        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        val notRoutableMessage = viewModel.state.value.errorMessage
        assertNotNull(notRoutableMessage)

        regionCatalog.regions = emptyList()
        viewModel.onAction(RouteRequestAction.OnRequestRoute)
        val outsideCoverageMessage = viewModel.state.value.errorMessage
        assertNotNull(outsideCoverageMessage)
        assertTrue(notRoutableMessage != outsideCoverageMessage)

        routingEngine.snappableLatitudes = setOf(1.0, 2.0)
        routingEngine.computePathResult = null
        viewModel.onAction(RouteRequestAction.OnRequestRoute)
        val noPathMessage = viewModel.state.value.errorMessage
        assertNotNull(noPathMessage)
        assertTrue(noPathMessage != notRoutableMessage && noPathMessage != outsideCoverageMessage)
    }
}
