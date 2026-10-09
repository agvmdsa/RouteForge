package com.routeforge.routing.presentation.routerequest.waypointedit

import com.routeforge.coredomain.model.Region
import com.routeforge.coredomain.model.RegionStatus
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.coredomain.usecase.ComputeRouteUseCase
import com.routeforge.coredomain.usecase.FreeRoamRouteBuilder
import com.routeforge.coredomain.usecase.PrepareRouteOptionsUseCase
import com.routeforge.coredomain.usecase.RecomputeRouteForBothModesUseCase
import com.routeforge.coredomain.usecase.RecordRegionUsageUseCase
import com.routeforge.routing.presentation.FakeRegionCatalog
import com.routeforge.routing.presentation.FakeRegionUsageTracker
import com.routeforge.routing.presentation.FakeRoutingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WaypointEditViewModelTest {
    private val routingEngine = FakeRoutingEngine()
    private val regionCatalog = FakeRegionCatalog()
    private val computeRoute = ComputeRouteUseCase(routingEngine, regionCatalog, RecordRegionUsageUseCase(FakeRegionUsageTracker()))
    private val recomputeRouteForBothModes =
        RecomputeRouteForBothModesUseCase(
            ComputeRequiredRegionsUseCase(regionCatalog),
            PrepareRouteOptionsUseCase(computeRoute, FreeRoamRouteBuilder()),
            computeRoute,
        )
    private val viewModel = WaypointEditViewModel(recomputeRouteForBothModes)

    private val points =
        listOf(
            RoutePoint(latitude = 0.0, longitude = 0.0),
            RoutePoint(latitude = 1.0, longitude = 0.0),
            RoutePoint(latitude = 2.0, longitude = 0.0),
        )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        routingEngine.snappableLatitudes = setOf(0.0, 1.0, 2.0)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reordering alone only updates the displayed order, without recomputing`() {
        viewModel.onAction(WaypointEditAction.OnOpen(points, RoutePlaybackMode.FREE_ROAM))

        viewModel.onAction(WaypointEditAction.OnReorder(fromIndex = 0, toIndex = 2))

        assertEquals(listOf(points[1], points[2], points[0]), viewModel.state.value.points)
        assertTrue(viewModel.state.value.hasUnappliedReorder)
    }

    @Test
    fun `a successful reorder updates the working list and emits Committed only once applied`() =
        runTest {
            routingEngine.computePathResult =
                Route(points = points, geometry = points.map { it.latitude to it.longitude }, distanceMeters = 1.0)
            viewModel.onAction(WaypointEditAction.OnOpen(points, RoutePlaybackMode.FREE_ROAM))
            viewModel.onAction(WaypointEditAction.OnReorder(fromIndex = 0, toIndex = 2))
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(WaypointEditAction.OnApplyChanges)

            val event = eventDeferred.await() as WaypointEditEvent.Committed
            assertEquals(listOf(points[1], points[2], points[0]), event.points)
            assertFalse(viewModel.state.value.hasUnappliedReorder)
        }

    @Test
    fun `a delete below the 2-waypoint floor is rejected`() {
        viewModel.onAction(WaypointEditAction.OnOpen(points.take(2), RoutePlaybackMode.FREE_ROAM))

        viewModel.onAction(WaypointEditAction.OnDelete("wp0"))

        assertEquals(2, viewModel.state.value.points.size)
    }

    @Test
    fun `before any mode has been chosen, Free-roam gatekeeps so Guided failure never reverts the edit`() =
        runTest {
            routingEngine.computePathResult = null // Guided can never succeed
            viewModel.onAction(WaypointEditAction.OnOpen(points, RoutePlaybackMode.FREE_ROAM))
            viewModel.onAction(WaypointEditAction.OnReorder(fromIndex = 0, toIndex = 2))
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(WaypointEditAction.OnApplyChanges)

            val event = eventDeferred.await() as WaypointEditEvent.Committed
            assertEquals(listOf(points[1], points[2], points[0]), event.points) // applied, not reverted
        }

    @Test
    fun `once a mode is chosen, that mode gatekeeps like Simulate's loaded route`() {
        routingEngine.computePathResult = null // Guided can never succeed
        viewModel.onAction(WaypointEditAction.OnOpen(points, RoutePlaybackMode.GUIDED))

        viewModel.onAction(WaypointEditAction.OnReorder(fromIndex = 0, toIndex = 2))
        viewModel.onAction(WaypointEditAction.OnApplyChanges)

        assertEquals(points, viewModel.state.value.points) // reverted
        assertNotNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `missing regions surfaces the summary instead of applying`() {
        regionCatalog.regions =
            listOf(
                Region(
                    id = "r1",
                    displayName = "Region 1",
                    minLatitude = -1.0,
                    minLongitude = -1.0,
                    maxLatitude = 3.0,
                    maxLongitude = 3.0,
                    tileIds = listOf("t1"),
                    approximateSizeBytes = 100L,
                    status = RegionStatus.NOT_DOWNLOADED,
                ),
            )
        viewModel.onAction(WaypointEditAction.OnOpen(points, RoutePlaybackMode.FREE_ROAM))

        viewModel.onAction(WaypointEditAction.OnReorder(fromIndex = 0, toIndex = 2))
        viewModel.onAction(WaypointEditAction.OnApplyChanges)

        assertNotNull(viewModel.state.value.missingRegionsWarning)
    }
}
