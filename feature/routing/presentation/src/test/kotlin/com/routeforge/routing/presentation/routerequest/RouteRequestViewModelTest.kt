package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.holder.LastKnownRealLocationHolder
import com.routeforge.coredomain.holder.SelectedFavoriteWaypointHolder
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.usecase.FreeRoamRouteBuilder
import com.routeforge.routing.domain.RouteFileCodec
import com.routeforge.coredomain.model.Region
import com.routeforge.coredomain.model.RegionStatus
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFailure
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.coredomain.usecase.ComputeRouteUseCase
import com.routeforge.routing.domain.usecase.ExportRouteFileUseCase
import com.routeforge.routing.domain.usecase.ImportRouteFileUseCase
import com.routeforge.coredomain.usecase.PrepareRouteOptionsUseCase
import com.routeforge.coredomain.usecase.RecordRegionUsageUseCase
import com.routeforge.routing.presentation.FakeFavoriteRoutesRepository
import com.routeforge.routing.presentation.FakeFavoriteWaypointsRepository
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    private val favoriteWaypointsRepository = FakeFavoriteWaypointsRepository()
    private val favoriteRoutesRepository = FakeFavoriteRoutesRepository()
    private val selectedFavoriteWaypointHolder = SelectedFavoriteWaypointHolder()

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
            favoriteWaypointsRepository = favoriteWaypointsRepository,
            favoriteRoutesRepository = favoriteRoutesRepository,
            selectedFavoriteWaypointHolder = selectedFavoriteWaypointHolder,
            backgroundDispatcher = dispatcher,
        )

    private fun RouteRequestViewModel.tapAndConfirm(
        latitude: Double,
        longitude: Double,
    ) {
        onAction(RouteRequestAction.OnMapTap(latitude, longitude))
        onAction(RouteRequestAction.OnConfirmAddWaypoint)
    }

    private fun RouteRequestViewModel.tapTwoPoints() {
        tapAndConfirm(1.0, 1.0)
        tapAndConfirm(2.0, 2.0)
    }

    // --- User Story 1: confirm before adding, optionally saving as a favorite ---

    @Test
    fun `tapping the map sets a pending add without adding the waypoint yet`() {
        val viewModel = createViewModel()

        viewModel.onAction(RouteRequestAction.OnMapTap(1.0, 1.0))

        assertEquals(emptyList<RoutePoint>(), viewModel.state.value.draft.points)
        assertEquals(1.0, viewModel.state.value.pendingAddLatitude)
        assertEquals(1.0, viewModel.state.value.pendingAddLongitude)
    }

    @Test
    fun `confirming without saving as a favorite adds only the waypoint`() {
        val viewModel = createViewModel()
        viewModel.onAction(RouteRequestAction.OnMapTap(1.0, 1.0))

        viewModel.onAction(RouteRequestAction.OnConfirmAddWaypoint)

        assertEquals(listOf(RoutePoint(1.0, 1.0)), viewModel.state.value.draft.points)
        assertEquals(emptyList<Any>(), favoriteWaypointsRepository.observeFavorites().value)
        assertNull(viewModel.state.value.pendingAddLatitude)
    }

    @Test
    fun `confirming with save-as-favorite checked and a name adds the waypoint and creates a favorite`() {
        val viewModel = createViewModel()
        viewModel.onAction(RouteRequestAction.OnMapTap(1.0, 2.0))
        viewModel.onAction(RouteRequestAction.OnToggleSaveAsFavorite)
        viewModel.onAction(RouteRequestAction.OnFavoriteNameInputChange("Home"))

        viewModel.onAction(RouteRequestAction.OnConfirmAddWaypoint)

        assertEquals(listOf(RoutePoint(1.0, 2.0)), viewModel.state.value.draft.points)
        val favorites = favoriteWaypointsRepository.observeFavorites().value
        assertEquals(1, favorites.size)
        assertEquals("Home", favorites[0].name)
        assertEquals(1.0, favorites[0].latitude)
        assertEquals(2.0, favorites[0].longitude)
    }

    @Test
    fun `confirming with save-as-favorite checked but a blank name does not proceed`() {
        val viewModel = createViewModel()
        viewModel.onAction(RouteRequestAction.OnMapTap(1.0, 1.0))
        viewModel.onAction(RouteRequestAction.OnToggleSaveAsFavorite)

        viewModel.onAction(RouteRequestAction.OnConfirmAddWaypoint)

        assertEquals(emptyList<RoutePoint>(), viewModel.state.value.draft.points)
        assertEquals(emptyList<Any>(), favoriteWaypointsRepository.observeFavorites().value)
        assertNotNull(viewModel.state.value.pendingAddLatitude)
    }

    @Test
    fun `dismissing the add-waypoint confirmation adds nothing and saves nothing`() {
        val viewModel = createViewModel()
        viewModel.onAction(RouteRequestAction.OnMapTap(1.0, 1.0))
        viewModel.onAction(RouteRequestAction.OnToggleSaveAsFavorite)
        viewModel.onAction(RouteRequestAction.OnFavoriteNameInputChange("Home"))

        viewModel.onAction(RouteRequestAction.OnDismissAddWaypoint)

        assertEquals(emptyList<RoutePoint>(), viewModel.state.value.draft.points)
        assertEquals(emptyList<Any>(), favoriteWaypointsRepository.observeFavorites().value)
        assertNull(viewModel.state.value.pendingAddLatitude)
        assertNull(viewModel.state.value.pendingAddLongitude)
        assertTrue(!viewModel.state.value.isSaveAsFavoriteChecked)
        assertEquals("", viewModel.state.value.favoriteNameInput)
    }

    @Test
    fun `selecting a favorite from the shared holder appends it to the draft`() {
        val viewModel = createViewModel()
        viewModel.onAction(RouteRequestAction.OnMapTap(1.0, 1.0))
        viewModel.onAction(RouteRequestAction.OnConfirmAddWaypoint)

        selectedFavoriteWaypointHolder.set(RoutePoint(9.0, 9.0))

        assertEquals(
            listOf(RoutePoint(1.0, 1.0), RoutePoint(9.0, 9.0)),
            viewModel.state.value.draft.points,
        )
        assertNull(selectedFavoriteWaypointHolder.selected.value)
    }

    // --- Favorite Routes: save a planned route (spec 005-favorite-routes, US1) ---

    @Test
    fun `save-route click opens the sheet`() {
        val viewModel = createViewModel()

        viewModel.onAction(RouteRequestAction.OnSaveRouteClick)

        assertTrue(viewModel.state.value.isSaveRouteSheetOpen)
    }

    @Test
    fun `confirming save-route with a blank name does not save and keeps the sheet open`() {
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        viewModel.onAction(RouteRequestAction.OnSaveRouteClick)

        viewModel.onAction(RouteRequestAction.OnConfirmSaveRoute)

        assertEquals(emptyList<Any>(), favoriteRoutesRepository.observeFavoriteRoutes().value)
        assertTrue(viewModel.state.value.isSaveRouteSheetOpen)
    }

    @Test
    fun `confirming save-route with a name saves the draft's current points in order and closes the sheet`() {
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        viewModel.onAction(RouteRequestAction.OnSaveRouteClick)
        viewModel.onAction(RouteRequestAction.OnRouteNameInputChange("Morning loop"))

        viewModel.onAction(RouteRequestAction.OnConfirmSaveRoute)

        val savedRoutes = favoriteRoutesRepository.observeFavoriteRoutes().value
        assertEquals(1, savedRoutes.size)
        assertEquals("Morning loop", savedRoutes[0].name)
        assertEquals(listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)), savedRoutes[0].points)
        assertTrue(!viewModel.state.value.isSaveRouteSheetOpen)
    }

    @Test
    fun `dismissing save-route saves nothing and resets the name input`() {
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()
        viewModel.onAction(RouteRequestAction.OnSaveRouteClick)
        viewModel.onAction(RouteRequestAction.OnRouteNameInputChange("Morning loop"))

        viewModel.onAction(RouteRequestAction.OnDismissSaveRoute)

        assertEquals(emptyList<Any>(), favoriteRoutesRepository.observeFavoriteRoutes().value)
        assertTrue(!viewModel.state.value.isSaveRouteSheetOpen)
        assertEquals("", viewModel.state.value.routeNameInput)
    }

    // --- User Story 2: manual point management ---

    @Test
    fun `tapping the map and confirming adds a numbered waypoint connected in order`() {
        val viewModel = createViewModel()

        viewModel.tapAndConfirm(1.0, 1.0)
        viewModel.tapAndConfirm(2.0, 2.0)

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
        viewModel.tapAndConfirm(3.0, 3.0)
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
        viewModel.tapAndConfirm(1.0, 1.0)
        viewModel.tapAndConfirm(2.0, 2.0)

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
        viewModel.tapAndConfirm(1.0, 1.0)

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

    @Test
    fun `opening the region catalog from the missing regions warning clears the warning and navigates`() =
        runTest(dispatcher) {
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

            val eventDeferred = async { viewModel.events.first() }
            viewModel.onAction(RouteRequestAction.OnOpenRegionCatalog)

            assertNull(viewModel.state.value.missingRegionsWarning)
            assertEquals(RouteRequestEvent.NavigateToRegionCatalog, eventDeferred.await())
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
    fun `dismissing the mode choice (tap-outside, back, or swipe) clears it instead of leaving the sheet stuck`() {
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
        assertNotNull(viewModel.state.value.routeOptions)

        viewModel.onAction(RouteRequestAction.OnDismissModeChoice)

        assertNull(viewModel.state.value.routeOptions)
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
    fun `choosing a mode when both are viable stores the other mode's path as the alternate`() {
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
        val freeRoam = viewModel.state.value.routeOptions?.freeRoam

        viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.GUIDED))

        val state = viewModel.state.value
        assertEquals(freeRoam?.geometry, state.route?.alternateGeometry)
        assertEquals(freeRoam?.distanceMeters, state.route?.alternateDistanceMeters)
    }

    @Test
    fun `auto-resolving to free-roam because guided isn't viable leaves no alternate to switch to`() {
        val viewModel = createViewModel()
        viewModel.tapTwoPoints()

        viewModel.onAction(RouteRequestAction.OnRequestRoute)

        val state = viewModel.state.value
        assertEquals(RoutePlaybackMode.FREE_ROAM, state.chosenMode)
        assertNull(state.route?.alternateGeometry)
        assertNull(state.route?.alternateDistanceMeters)
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
    fun `OnUseRoute opens the go-to-Simulate confirmation instead of navigating immediately`() =
        runTest(dispatcher) {
            routingEngine.snappableLatitudes = setOf(1.0, 2.0)
            routingEngine.computePathResult =
                Route(points = emptyList(), geometry = listOf(1.0 to 1.0, 2.0 to 2.0), distanceMeters = 10.0)
            val viewModel = createViewModel()
            viewModel.tapTwoPoints()
            viewModel.onAction(RouteRequestAction.OnRequestRoute)
            viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.FREE_ROAM))

            viewModel.onAction(RouteRequestAction.OnUseRoute)

            assertTrue(viewModel.state.value.pendingGoToSimulateConfirmation)
        }

    @Test
    fun `OnConfirmGoToSimulate sends GoToSimulate and clears the confirmation`() =
        runTest(dispatcher) {
            routingEngine.snappableLatitudes = setOf(1.0, 2.0)
            routingEngine.computePathResult =
                Route(points = emptyList(), geometry = listOf(1.0 to 1.0, 2.0 to 2.0), distanceMeters = 10.0)
            val viewModel = createViewModel()
            viewModel.tapTwoPoints()
            viewModel.onAction(RouteRequestAction.OnRequestRoute)
            viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.FREE_ROAM))
            viewModel.onAction(RouteRequestAction.OnUseRoute)

            val eventDeferred = async { viewModel.events.first() }
            viewModel.onAction(RouteRequestAction.OnConfirmGoToSimulate)

            assertEquals(RouteRequestEvent.GoToSimulate, eventDeferred.await())
            assertFalse(viewModel.state.value.pendingGoToSimulateConfirmation)
        }

    @Test
    fun `OnDismissGoToSimulateConfirmation clears the confirmation without navigating`() =
        runTest(dispatcher) {
            routingEngine.snappableLatitudes = setOf(1.0, 2.0)
            routingEngine.computePathResult =
                Route(points = emptyList(), geometry = listOf(1.0 to 1.0, 2.0 to 2.0), distanceMeters = 10.0)
            val viewModel = createViewModel()
            viewModel.tapTwoPoints()
            viewModel.onAction(RouteRequestAction.OnRequestRoute)
            viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.FREE_ROAM))
            viewModel.onAction(RouteRequestAction.OnUseRoute)

            viewModel.onAction(RouteRequestAction.OnDismissGoToSimulateConfirmation)

            assertFalse(viewModel.state.value.pendingGoToSimulateConfirmation)
            assertNotNull(viewModel.state.value.route)
        }

    @Test
    fun `dismissing the go-to-Simulate confirmation keeps the route available for a later manual switch`() =
        runTest(dispatcher) {
            routingEngine.snappableLatitudes = setOf(1.0, 2.0)
            routingEngine.computePathResult =
                Route(points = emptyList(), geometry = listOf(1.0 to 1.0, 2.0 to 2.0), distanceMeters = 10.0)
            val viewModel = createViewModel()
            viewModel.tapTwoPoints()
            viewModel.onAction(RouteRequestAction.OnRequestRoute)
            viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.FREE_ROAM))
            viewModel.onAction(RouteRequestAction.OnUseRoute)
            viewModel.onAction(RouteRequestAction.OnDismissGoToSimulateConfirmation)

            assertNotNull(lastComputedRouteHolder.route.value)
        }

    @Test
    fun `applying a committed reorder-list edit invalidates an already-computed route, per FR-018`() =
        runTest(dispatcher) {
            routingEngine.snappableLatitudes = setOf(1.0, 2.0)
            routingEngine.computePathResult =
                Route(points = emptyList(), geometry = listOf(1.0 to 1.0, 2.0 to 2.0), distanceMeters = 10.0)
            val viewModel = createViewModel()
            viewModel.tapTwoPoints()
            viewModel.onAction(RouteRequestAction.OnRequestRoute)
            viewModel.onAction(RouteRequestAction.OnChooseMode(RoutePlaybackMode.FREE_ROAM))
            assertNotNull(viewModel.state.value.route)

            viewModel.applyEditedWaypoints(listOf(RoutePoint(2.0, 2.0), RoutePoint(1.0, 1.0)))

            assertNull(viewModel.state.value.route)
            assertNull(viewModel.state.value.routeOptions)
            assertEquals(listOf(RoutePoint(2.0, 2.0), RoutePoint(1.0, 1.0)), viewModel.state.value.draft.points)
        }
}
