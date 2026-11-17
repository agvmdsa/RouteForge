package com.routeforge.simulation.presentation.waypointedit

import com.routeforge.coredomain.AddressLookup
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.AddressLabel
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.NamedPlace
import com.routeforge.coredomain.model.Region
import com.routeforge.coredomain.model.RegionStatus
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.testing.FakeFavoriteWaypointsRepository
import com.routeforge.coredomain.testing.FakeRecentPlacesRepository
import com.routeforge.coredomain.testing.FakeRegionCatalog
import com.routeforge.coredomain.testing.FakeRegionUsageTracker
import com.routeforge.coredomain.testing.FakeReverseGeocoder
import com.routeforge.coredomain.testing.FakeRoutingEngine
import com.routeforge.coredomain.usecase.ComputeEditableWaypointRangeUseCase
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.coredomain.usecase.ComputeRouteUseCase
import com.routeforge.coredomain.usecase.FreeRoamRouteBuilder
import com.routeforge.coredomain.usecase.PrepareRouteOptionsUseCase
import com.routeforge.coredomain.usecase.RecomputeRouteForBothModesUseCase
import com.routeforge.coredomain.usecase.RecordRegionUsageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val computeEditableWaypointRange = ComputeEditableWaypointRangeUseCase()
    private val favoriteWaypointsRepository = FakeFavoriteWaypointsRepository()
    private val recentPlaces = FakeRecentPlacesRepository()
    private val reverseGeocoder = FakeReverseGeocoder()
    private val dispatcher = UnconfinedTestDispatcher()
    private val viewModel =
        WaypointEditViewModel(
            recomputeRouteForBothModes,
            computeEditableWaypointRange,
            favoriteWaypointsRepository,
            recentPlaces,
            AddressLookup(reverseGeocoder),
        )

    private val points =
        listOf(
            RoutePoint(latitude = 0.0, longitude = 0.0),
            RoutePoint(latitude = 1.0, longitude = 0.0),
            RoutePoint(latitude = 2.0, longitude = 0.0),
        )
    private val loadedRoute =
        Route(
            points = points,
            geometry = points.map { LatLng(it.latitude, it.longitude) },
            distanceMeters = 200.0,
            mode = RoutePlaybackMode.FREE_ROAM,
            waypointCumulativeDistances = listOf(0.0, 100.0, 200.0),
        )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        routingEngine.computePathResult =
            Route(
                points = points,
                geometry = points.map { LatLng(it.latitude, it.longitude) },
                distanceMeters = 1.0,
            )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening while loaded-not-running makes every waypoint editable`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))

        assertEquals(0, viewModel.state.value.firstEditableIndex)
        assertEquals(points, viewModel.state.value.points)
    }

    @Test
    fun `opening while a session is active computes the boundary from waypointCumulativeDistances`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = 150.0))

        assertEquals(2, viewModel.state.value.firstEditableIndex)
        assertTrue(viewModel.state.value.isSessionActive)
    }

    @Test
    fun `reordering alone only updates the displayed order, without recomputing`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))

        viewModel.onAction(WaypointEditAction.OnReorder(fromIndex = 0, toIndex = 2))

        assertEquals(listOf(points[1], points[2], points[0]), viewModel.state.value.points)
        assertTrue(viewModel.state.value.hasUnappliedReorder)
    }

    @Test
    fun `a successful reorder updates the working list and emits Committed only once applied`() =
        runTest {
            viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))
            viewModel.onAction(WaypointEditAction.OnReorder(fromIndex = 0, toIndex = 2))
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(WaypointEditAction.OnApplyChanges)

            val event = eventDeferred.await() as WaypointEditEvent.Committed
            assertEquals(listOf(points[1], points[2], points[0]), event.points)
            assertEquals(event.points, viewModel.state.value.points)
            assertFalse(viewModel.state.value.hasUnappliedReorder)
        }

    @Test
    fun `an active-mode failure reverts the working list and surfaces an error`() =
        runTest {
            // Free-roam (loadedRoute's mode) always succeeds via its own builder, so force the
            // active-mode failure path via Guided instead, whose geometry depends on routingEngine.
            routingEngine.computePathResult = null
            val guidedRoute = loadedRoute.copy(mode = RoutePlaybackMode.GUIDED)
            viewModel.onAction(WaypointEditAction.OnOpen(guidedRoute, distanceTraveledMeters = null))

            viewModel.onAction(WaypointEditAction.OnReorder(fromIndex = 0, toIndex = 2))
            viewModel.onAction(WaypointEditAction.OnApplyChanges)

            assertEquals(points, viewModel.state.value.points) // unchanged
            assertNotNull(viewModel.state.value.errorMessage)
        }

    @Test
    fun `missing regions surfaces the summary instead of applying`() =
        runTest {
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
            viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))

            viewModel.onAction(WaypointEditAction.OnReorder(fromIndex = 0, toIndex = 2))
            viewModel.onAction(WaypointEditAction.OnApplyChanges)

            assertNotNull(viewModel.state.value.missingRegionsWarning)
        }

    @Test
    fun `closing the sheet clears all edit-mode state`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))

        viewModel.onAction(WaypointEditAction.OnClose)

        assertEquals(WaypointEditState(), viewModel.state.value)
    }

    @Test
    fun `a delete that would drop the editable count to 1 is rejected without a recompute`() =
        runTest {
            val twoPointRoute = loadedRoute.copy(points = points.take(2), waypointCumulativeDistances = listOf(0.0, 100.0))
            viewModel.onAction(WaypointEditAction.OnOpen(twoPointRoute, distanceTraveledMeters = null))

            viewModel.onAction(WaypointEditAction.OnDelete(0))

            assertEquals(2, viewModel.state.value.points.size) // unchanged — rejected before any recompute
        }

    @Test
    fun `an edit targeting a read-only index below the boundary is rejected`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = 150.0)) // firstEditableIndex == 2

        viewModel.onAction(WaypointEditAction.OnMarkerDragged(index = 0, latitude = 9.0, longitude = 9.0))

        assertEquals(points, viewModel.state.value.points) // unchanged
    }

    @Test
    fun `a missing-regions result during an active session requests an auto-pause on go-to-downloads`() =
        runTest {
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
            viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = 50.0))
            viewModel.onAction(WaypointEditAction.OnReorder(fromIndex = 1, toIndex = 2))
            viewModel.onAction(WaypointEditAction.OnApplyChanges)
            assertNotNull(viewModel.state.value.missingRegionsWarning)

            val events = mutableListOf<WaypointEditEvent>()
            // Collect eagerly: the events channel is a rendezvous, and a collector that only starts when the test body yields
            // would not be there when the view model sends.
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events.add(it) } }

            viewModel.onAction(WaypointEditAction.OnGoToDownloads)

            assertTrue(events.contains(WaypointEditEvent.GoToDownloads))
            assertTrue(events.contains(WaypointEditEvent.AutoPauseRequested))
            assertNull(viewModel.state.value.missingRegionsWarning)
            job.cancel()
        }

    @Test
    fun `adding a tapped spot saved as a favorite records the place under the favorite name`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))
        viewModel.onAction(WaypointEditAction.OnMapTap(latitude = 3.0, longitude = 0.0))
        viewModel.onAction(WaypointEditAction.OnToggleSaveAsFavorite)
        viewModel.onAction(WaypointEditAction.OnFavoriteNameChange("Edit spot"))

        viewModel.onAction(WaypointEditAction.OnConfirmAdd)

        assertEquals(listOf(NamedPlace(latitude = 3.0, longitude = 0.0, name = "Edit spot")), recentPlaces.recorded)
    }

    @Test
    fun `adding a tapped spot without the favorite box records nothing`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))
        viewModel.onAction(WaypointEditAction.OnMapTap(latitude = 3.0, longitude = 0.0))

        viewModel.onAction(WaypointEditAction.OnConfirmAdd)

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `adding a tapped spot with the favorite box ticked and no name records nothing and saves no favorite`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))
        viewModel.onAction(WaypointEditAction.OnMapTap(latitude = 3.0, longitude = 0.0))
        viewModel.onAction(WaypointEditAction.OnToggleSaveAsFavorite)

        viewModel.onAction(WaypointEditAction.OnConfirmAdd)

        assertTrue(recentPlaces.recorded.isEmpty())
        assertTrue(favoriteWaypointsRepository.observeFavorites().value.isEmpty())
    }

    @Test
    fun `dismissing the add confirmation records nothing`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))
        viewModel.onAction(WaypointEditAction.OnMapTap(latitude = 3.0, longitude = 0.0))
        viewModel.onAction(WaypointEditAction.OnToggleSaveAsFavorite)
        viewModel.onAction(WaypointEditAction.OnFavoriteNameChange("Edit spot"))

        viewModel.onAction(WaypointEditAction.OnDismissAdd)

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `dragging a marker records nothing`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))

        viewModel.onAction(WaypointEditAction.OnMarkerDragged(index = 1, latitude = 1.5, longitude = 0.5))

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `reordering and applying the change records nothing`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))
        viewModel.onAction(WaypointEditAction.OnReorder(fromIndex = 0, toIndex = 2))

        viewModel.onAction(WaypointEditAction.OnApplyChanges)

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `deleting a waypoint records nothing`() {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))

        viewModel.onAction(WaypointEditAction.OnDelete(0))

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    private val address = AddressLabel(full = "Pariser Platz 1, Mitte, Berlin", short = "Pariser Platz 1")

    private fun passTheLookupPause() {
        dispatcher.scheduler.advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS)
        dispatcher.scheduler.runCurrent()
    }

    private fun openAndTap(
        latitude: Double = 3.0,
        longitude: Double = 0.0,
    ) {
        viewModel.onAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters = null))
        viewModel.onAction(WaypointEditAction.OnMapTap(latitude = latitude, longitude = longitude))
    }

    @Test
    fun `tapping the map shows the address of the spot after the pause`() {
        reverseGeocoder.answer = Result.Success(address)
        openAndTap()
        assertNull(viewModel.state.value.pendingAddAddress)

        passTheLookupPause()

        assertEquals(address, viewModel.state.value.pendingAddAddress)
        assertEquals(listOf(3.0 to 0.0), reverseGeocoder.requests)
    }

    @Test
    fun `a failed lookup leaves no address and adding the waypoint still closes the confirmation at once`() {
        reverseGeocoder.answer = Result.Error(DataError.Network.NO_INTERNET)
        openAndTap()
        passTheLookupPause()

        viewModel.onAction(WaypointEditAction.OnConfirmAdd)

        assertNull(viewModel.state.value.pendingAddAddress)
        assertNull(viewModel.state.value.pendingAddLatitude)
    }

    @Test
    fun `an answer of nothing found leaves no address`() {
        reverseGeocoder.answer = Result.Success(null)
        openAndTap()

        passTheLookupPause()

        assertEquals(1, reverseGeocoder.requests.size)
        assertNull(viewModel.state.value.pendingAddAddress)
    }

    @Test
    fun `confirming before the address arrives discards it`() {
        reverseGeocoder.answer = Result.Success(address)
        reverseGeocoder.hold()
        openAndTap()
        passTheLookupPause()

        viewModel.onAction(WaypointEditAction.OnConfirmAdd)
        reverseGeocoder.release()
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.state.value.pendingAddAddress)
    }

    @Test
    fun `dismissing before the address arrives discards it`() {
        reverseGeocoder.answer = Result.Success(address)
        reverseGeocoder.hold()
        openAndTap()
        passTheLookupPause()

        viewModel.onAction(WaypointEditAction.OnDismissAdd)
        reverseGeocoder.release()
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.state.value.pendingAddAddress)
    }

    @Test
    fun `closing the sheet before the address arrives discards it`() {
        reverseGeocoder.answer = Result.Success(address)
        reverseGeocoder.hold()
        openAndTap()
        passTheLookupPause()

        viewModel.onAction(WaypointEditAction.OnClose)
        reverseGeocoder.release()
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.state.value.pendingAddAddress)
    }

    @Test
    fun `a new map tap clears the earlier address until its own arrives`() {
        reverseGeocoder.answer = Result.Success(address)
        openAndTap()
        passTheLookupPause()
        assertEquals(address, viewModel.state.value.pendingAddAddress)

        viewModel.onAction(WaypointEditAction.OnMapTap(latitude = 4.0, longitude = 0.0))

        assertNull(viewModel.state.value.pendingAddAddress)
    }

    @Test
    fun `an address arriving while the favorite name is untouched is offered as the name`() {
        reverseGeocoder.answer = Result.Success(address)
        openAndTap()

        passTheLookupPause()

        assertEquals("Pariser Platz 1", viewModel.state.value.favoriteNameInput)
    }

    @Test
    fun `an address arriving after the user typed a favorite name does not replace it`() {
        reverseGeocoder.answer = Result.Success(address)
        openAndTap()
        viewModel.onAction(WaypointEditAction.OnToggleSaveAsFavorite)
        viewModel.onAction(WaypointEditAction.OnFavoriteNameChange("My spot"))

        passTheLookupPause()

        assertEquals("My spot", viewModel.state.value.favoriteNameInput)
    }

    @Test
    fun `clearing the favorite name keeps the address from filling it`() {
        reverseGeocoder.answer = Result.Success(address)
        openAndTap()
        viewModel.onAction(WaypointEditAction.OnToggleSaveAsFavorite)
        viewModel.onAction(WaypointEditAction.OnFavoriteNameChange("x"))
        viewModel.onAction(WaypointEditAction.OnFavoriteNameChange(""))

        passTheLookupPause()

        assertEquals("", viewModel.state.value.favoriteNameInput)
    }

    @Test
    fun `ticking the favorite box leaves the offered name in place and saving uses it`() {
        reverseGeocoder.answer = Result.Success(address)
        openAndTap()
        passTheLookupPause()

        viewModel.onAction(WaypointEditAction.OnToggleSaveAsFavorite)
        assertEquals("Pariser Platz 1", viewModel.state.value.favoriteNameInput)
        viewModel.onAction(WaypointEditAction.OnConfirmAdd)

        assertEquals(listOf("Pariser Platz 1"), favoriteWaypointsRepository.observeFavorites().value.map { it.name })
    }

    @Test
    fun `dismissing resets the favorite name and the touched mark`() {
        openAndTap()
        viewModel.onAction(WaypointEditAction.OnToggleSaveAsFavorite)
        viewModel.onAction(WaypointEditAction.OnFavoriteNameChange("My spot"))

        viewModel.onAction(WaypointEditAction.OnDismissAdd)

        assertEquals("", viewModel.state.value.favoriteNameInput)
        assertFalse(viewModel.state.value.isFavoriteNameTouched)
    }

    @Test
    fun `a new map tap resets the favorite name and lets its own address be offered`() {
        reverseGeocoder.answer = Result.Success(address)
        openAndTap()
        viewModel.onAction(WaypointEditAction.OnToggleSaveAsFavorite)
        viewModel.onAction(WaypointEditAction.OnFavoriteNameChange("My spot"))

        viewModel.onAction(WaypointEditAction.OnMapTap(latitude = 4.0, longitude = 0.0))
        assertEquals("", viewModel.state.value.favoriteNameInput)
        assertFalse(viewModel.state.value.isFavoriteNameTouched)
        passTheLookupPause()

        assertEquals("Pariser Platz 1", viewModel.state.value.favoriteNameInput)
    }

}
