package com.routeforge.routing.presentation

import com.routeforge.coredomain.DraftWaypointsHolder
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.coredomain.LastKnownRealLocationHolder
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.FreeRoamRouteBuilder
import com.routeforge.routing.domain.RouteFileCodec
import com.routeforge.routing.domain.RoutingEngine
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFailure
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.routing.domain.usecase.ComputeRouteUseCase
import com.routeforge.routing.domain.usecase.ExportRouteFileUseCase
import com.routeforge.routing.domain.usecase.ImportRouteFileUseCase
import com.routeforge.routing.domain.usecase.PrepareRouteOptionsUseCase
import com.routeforge.routing.domain.usecase.RecordRegionUsageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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

private class FakeRouteFileCodec(
    var parseResult: Result<RouteDraft, RouteFileFailure> = Result.Success(RouteDraft()),
) : RouteFileCodec {
    var lastSerialized: Route? = null

    override fun parse(bytes: ByteArray): Result<RouteDraft, RouteFileFailure> = parseResult

    override fun serialize(route: Route): ByteArray {
        lastSerialized = route
        return byteArrayOf(1, 2, 3)
    }
}

class RouteRequestViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val routingEngine = FakeRoutingEngine()
    private val regionCatalog = FakeRegionCatalog()
    private val lastComputedRouteHolder = LastComputedRouteHolder()
    private val jsonCodec = FakeRouteFileCodec()
    private val gpxCodec = FakeRouteFileCodec()
    private val codecs = mapOf(RouteFileFormat.JSON to jsonCodec, RouteFileFormat.GPX to gpxCodec)

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
            prepareRouteOptions =
                PrepareRouteOptionsUseCase(
                    computeRoute = ComputeRouteUseCase(routingEngine, regionCatalog, RecordRegionUsageUseCase(FakeRegionUsageTracker())),
                    freeRoamRouteBuilder = FreeRoamRouteBuilder(),
                ),
            lastComputedRouteHolder = lastComputedRouteHolder,
            importRouteFile = ImportRouteFileUseCase(codecs),
            exportRouteFile = ExportRouteFileUseCase(codecs),
            lastKnownRealLocationHolder = LastKnownRealLocationHolder(),
            computeRequiredRegions = ComputeRequiredRegionsUseCase(regionCatalog),
            draftWaypointsHolder = DraftWaypointsHolder(),
            backgroundDispatcher = dispatcher,
        )

    private fun RouteRequestViewModel.tapTwoPoints() {
        onAction(RouteRequestAction.OnMapTap(1.0, 1.0))
        onAction(RouteRequestAction.OnMapTap(2.0, 2.0))
    }

    // --- User Story 2: manual point management ---

    @Test
    fun `tapping the map adds a numbered waypoint connected in order`() {
        val viewModel = createViewModel()

        viewModel.onAction(RouteRequestAction.OnMapTap(1.0, 1.0))
        viewModel.onAction(RouteRequestAction.OnMapTap(2.0, 2.0))

        assertEquals(
            listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)),
            viewModel.state.value.draft.points,
        )
    }

    @Test
    fun `dragging a marker updates its position`() {
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()

        viewModel.onAction(RouteRequestAction.OnMarkerDragged(index = 1, latitude = 9.0, longitude = 9.0))

        assertEquals(
            listOf(RoutePoint(1.0, 1.0), RoutePoint(9.0, 9.0)),
            viewModel.state.value.draft.points,
        )
    }

    @Test
    fun `clicking a marker opens the edit dialog prefilled with its coordinates`() {
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()

        viewModel.onAction(RouteRequestAction.OnMarkerClick(0))

        val state = viewModel.state.value
        assertEquals(0, state.editingIndex)
        assertEquals("1.0", state.editLatitudeInput)
        assertEquals("1.0", state.editLongitudeInput)
    }

    @Test
    fun `confirming an edit replaces the waypoint's coordinates and closes the dialog`() {
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        viewModel.onAction(RouteRequestAction.OnMarkerClick(0))

        viewModel.onAction(RouteRequestAction.OnEditLatitudeChange("5.0"))
        viewModel.onAction(RouteRequestAction.OnEditLongitudeChange("5.0"))
        viewModel.onAction(RouteRequestAction.OnConfirmEdit)

        val state = viewModel.state.value
        assertEquals(RoutePoint(5.0, 5.0), state.draft.points[0])
        assertNull(state.editingIndex)
    }

    @Test
    fun `deleting the waypoint being edited removes it and keeps the rest connected`() {
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        viewModel.onAction(RouteRequestAction.OnMapTap(3.0, 3.0))
        viewModel.onAction(RouteRequestAction.OnMarkerClick(1))

        viewModel.onAction(RouteRequestAction.OnDeleteEditingWaypoint)

        assertEquals(
            listOf(RoutePoint(1.0, 1.0), RoutePoint(3.0, 3.0)),
            viewModel.state.value.draft.points,
        )
        assertNull(viewModel.state.value.editingIndex)
    }

    @Test
    fun `undo reverts the most recent change and repeated undo is a no-op once history is empty`() {
        val viewModel = createViewModel()
        viewModel.onAction(RouteRequestAction.OnMapTap(1.0, 1.0))
        viewModel.onAction(RouteRequestAction.OnMapTap(2.0, 2.0))

        viewModel.onAction(RouteRequestAction.OnUndo)
        assertEquals(listOf(RoutePoint(1.0, 1.0)), viewModel.state.value.draft.points)

        viewModel.onAction(RouteRequestAction.OnUndo)
        assertEquals(emptyList<RoutePoint>(), viewModel.state.value.draft.points)

        viewModel.onAction(RouteRequestAction.OnUndo)
        assertEquals(emptyList<RoutePoint>(), viewModel.state.value.draft.points)
    }

    @Test
    fun `proceeding to play with fewer than 2 points is blocked with a clear message`() {
        val viewModel = createViewModel()
        viewModel.onAction(RouteRequestAction.OnMapTap(1.0, 1.0))

        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        assertNotNull(viewModel.state.value.errorType)
        assertNull(viewModel.state.value.routeOptions)
        assertNull(viewModel.state.value.route)
    }

    // --- Region-awareness before play ---

    @Test
    fun `waypoints with no matching region data at all proceed straight to computation`() {
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()

        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        assertNull(viewModel.state.value.missingRegionsWarning)
        assertNotNull(viewModel.state.value.route)
    }

    @Test
    fun `an undownloaded region covering a waypoint blocks play with a warning instead of computing`() {
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
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()

        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        assertNotNull(viewModel.state.value.missingRegionsWarning)
        assertNull(viewModel.state.value.route)
        assertNull(viewModel.state.value.routeOptions)
    }

    @Test
    fun `continuing anyway from the missing regions warning proceeds with computation`() {
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
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        viewModel.onAction(RouteRequestAction.OnProceedDespiteMissingRegions)

        assertNull(viewModel.state.value.missingRegionsWarning)
        assertNotNull(viewModel.state.value.route)
    }

    // --- User Story 1: mode gate ---

    @Test
    fun `both modes are offered when guided is fully computable, and route stays null until a mode is chosen`() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0)
        routingEngine.computePathResult =
            Route(
                points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)),
                geometry = listOf(1.0 to 1.0, 2.0 to 2.0),
                distanceMeters = 100.0,
            )
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()

        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        val state = viewModel.state.value
        assertNotNull(state.routeOptions?.guided)
        assertNotNull(state.routeOptions?.freeRoam)
        assertNull(state.route)
        assertNull(state.chosenMode)
        assertTrue(!state.isComputing)
    }

    @Test
    fun `choosing guided finalizes the guided route and locks the mode`() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0)
        routingEngine.computePathResult =
            Route(
                points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)),
                geometry = listOf(1.0 to 1.0, 2.0 to 2.0),
                distanceMeters = 100.0,
            )
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.GUIDED))

        val state = viewModel.state.value
        assertEquals(RoutePlaybackMode.GUIDED, state.route?.mode)
        assertEquals(RoutePlaybackMode.GUIDED, state.chosenMode)
        assertNull(state.routeOptions)
    }

    @Test
    fun `choosing free-roam finalizes the free-roam route even when guided was also available`() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0)
        routingEngine.computePathResult =
            Route(
                points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)),
                geometry = listOf(1.0 to 1.0, 2.0 to 2.0),
                distanceMeters = 100.0,
            )
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.FREE_ROAM))

        val state = viewModel.state.value
        assertEquals(RoutePlaybackMode.FREE_ROAM, state.route?.mode)
        assertEquals(RoutePlaybackMode.FREE_ROAM, state.chosenMode)
    }

    @Test
    fun `an unroutable point skips straight to free-roam with no error shown`() {
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()

        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        val state = viewModel.state.value
        assertEquals(RoutePlaybackMode.FREE_ROAM, state.chosenMode)
        assertNotNull(state.route)
        assertNull(state.routeOptions)
        assertNull(state.errorType)
    }

    @Test
    fun `no connecting path between snapped points also skips straight to free-roam`() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0)
        routingEngine.computePathResult = null
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()

        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        assertEquals(RoutePlaybackMode.FREE_ROAM, viewModel.state.value.chosenMode)
        assertNull(viewModel.state.value.errorType)
    }

    @Test
    fun `re-running the check clears a previously locked mode`() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0)
        routingEngine.computePathResult =
            Route(points = emptyList(), geometry = listOf(1.0 to 1.0, 2.0 to 2.0), distanceMeters = 10.0)
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        viewModel.onAction(RouteRequestAction.OnRequestRoute)
        viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.GUIDED))
        assertNotNull(viewModel.state.value.chosenMode)

        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        assertNull(viewModel.state.value.chosenMode)
        assertNull(viewModel.state.value.route)
        assertNotNull(viewModel.state.value.routeOptions)
    }

    // --- User Story 3: file import/export ---

    @Test
    fun `a successful import replaces the current draft and clears any prior route`() {
        val imported = RouteDraft().add(RoutePoint(9.0, 9.0)).add(RoutePoint(8.0, 8.0))
        jsonCodec.parseResult = Result.Success(imported)
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()

        viewModel.onAction(RouteRequestAction.OnRouteFileImported(byteArrayOf(0), RouteFileFormat.JSON))

        val state = viewModel.state.value
        assertEquals(imported.points, state.draft.points)
        assertNull(state.errorType)
    }

    @Test
    fun `a failed import surfaces a clear error and leaves the draft untouched`() {
        jsonCodec.parseResult = Result.Error(RouteFileFailure.TooFewWaypoints)
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        val pointsBefore = viewModel.state.value.draft.points

        viewModel.onAction(RouteRequestAction.OnRouteFileImported(byteArrayOf(0), RouteFileFormat.JSON))

        assertNotNull(viewModel.state.value.errorType)
        assertEquals(pointsBefore, viewModel.state.value.draft.points)
    }

    @Test
    fun `exporting a finalized route serializes it through the matching codec`() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0)
        routingEngine.computePathResult =
            Route(points = emptyList(), geometry = listOf(1.0 to 1.0, 2.0 to 2.0), distanceMeters = 10.0)
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        viewModel.onAction(RouteRequestAction.OnRequestRoute)
        viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.FREE_ROAM))

        viewModel.onAction(RouteRequestAction.OnExportRoute(RouteFileFormat.GPX))

        assertEquals(viewModel.state.value.route, gpxCodec.lastSerialized)
        assertNull(jsonCodec.lastSerialized)
    }

    @Test
    fun `choosing a mode finalizes the route but does not navigate away by itself`() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0)
        routingEngine.computePathResult =
            Route(points = emptyList(), geometry = listOf(1.0 to 1.0, 2.0 to 2.0), distanceMeters = 10.0)
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.FREE_ROAM))

        assertNotNull(viewModel.state.value.route)
    }

    @Test
    fun `OnUseRoute sends the RouteComputed event only after a mode has been chosen`() =
        runTest(dispatcher) {
            routingEngine.snappableLatitudes = setOf(1.0, 2.0)
            routingEngine.computePathResult =
                Route(points = emptyList(), geometry = listOf(1.0 to 1.0, 2.0 to 2.0), distanceMeters = 10.0)
            val viewModel = createViewModel()
            viewModel.tapTwoPoints()
            viewModel.onAction(RouteRequestAction.OnRequestRoute)
            viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.FREE_ROAM))

            val eventDeferred = async { viewModel.events.first() }
            viewModel.onAction(RouteRequestAction.OnUseRoute)

            assertEquals(viewModel.state.value.route, (eventDeferred.await() as RouteRequestEvent.RouteComputed).route)
        }
}
