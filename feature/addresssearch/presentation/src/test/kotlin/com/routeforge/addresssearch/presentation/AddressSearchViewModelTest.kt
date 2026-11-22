package com.routeforge.addresssearch.presentation

import com.routeforge.addresssearch.domain.testing.FakeGeocodeClient
import com.routeforge.addresssearch.domain.usecase.RankSearchResultsUseCase
import com.routeforge.addresssearch.domain.usecase.SearchPlacesUseCase
import com.routeforge.coredomain.AddressLookup
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.FavoriteWaypointsRepository
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.handoff.LastKnownRealLocationHandoffImpl
import com.routeforge.coredomain.handoff.PendingSearchWaypointHandoffImpl
import com.routeforge.coredomain.handoff.PendingTeleportTargetHandoffImpl
import com.routeforge.coredomain.model.AddressLabel
import com.routeforge.coredomain.model.FavoriteWaypoint
import com.routeforge.coredomain.model.NamedPlace
import com.routeforge.coredomain.model.PlaceSearchResult
import com.routeforge.coredomain.testing.FakeRecentPlacesRepository
import com.routeforge.coredomain.testing.FakeReverseGeocoder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private fun place(
    name: String = "Place",
    latitude: Double = 1.0,
    longitude: Double = 2.0,
) = PlaceSearchResult(name = name, formattedAddress = name, latitude = latitude, longitude = longitude, importance = 0.5)

private class FakeFavoriteWaypointsRepository : FavoriteWaypointsRepository {
    val added = mutableListOf<Triple<String, Double, Double>>()
    private val favoritesState = MutableStateFlow(emptyList<FavoriteWaypoint>())

    override fun observeFavorites(): StateFlow<List<FavoriteWaypoint>> = favoritesState

    override fun add(
        name: String,
        latitude: Double,
        longitude: Double,
    ): FavoriteWaypoint {
        added += Triple(name, latitude, longitude)
        return FavoriteWaypoint(id = "fake", name = name, latitude = latitude, longitude = longitude)
    }

    override fun update(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double,
    ) = Unit

    override fun delete(id: String) = Unit
}

class AddressSearchViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val geocodeClient = FakeGeocodeClient(Result.Success(listOf(place())))
    private val pendingSearchWaypoint = PendingSearchWaypointHandoffImpl()
    private val pendingTeleportTarget = PendingTeleportTargetHandoffImpl()
    private val favoriteWaypointsRepository = FakeFavoriteWaypointsRepository()
    private val recentPlaces = FakeRecentPlacesRepository()
    private val reverseGeocoder = FakeReverseGeocoder()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        sourceContext: SearchSourceContext = SearchSourceContext.TELEPORT_OR_FAVORITE,
        initialPlace: InitialPlace? = null,
        initialQuery: String? = null,
    ): AddressSearchViewModel =
        AddressSearchViewModel(
            sourceContext = sourceContext,
            referenceLatitude = null,
            referenceLongitude = null,
            initialPlace = initialPlace,
            initialQuery = initialQuery,
            searchPlaces = SearchPlacesUseCase(geocodeClient, RankSearchResultsUseCase(), LastKnownRealLocationHandoffImpl()),
            pendingSearchWaypointSender = pendingSearchWaypoint,
            pendingTeleportTargetSender = pendingTeleportTarget,
            favoriteWaypointsRepository = favoriteWaypointsRepository,
            recentPlacesRepository = recentPlaces,
            addressLookup = AddressLookup(reverseGeocoder),
        )

    @Test
    fun `a blank query does not trigger a search`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onAction(AddressSearchAction.OnSubmit)

            assertFalse(viewModel.state.value.hasSearched)
        }

    @Test
    fun `a successful search populates results`() =
        runTest {
            val result = place(name = "Found place")
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel()

            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)

            assertEquals(listOf(result), viewModel.state.value.results)
            assertTrue(viewModel.state.value.hasSearched)
            assertNull(viewModel.state.value.errorType)
        }

    @Test
    fun `a failed search surfaces a network error`() =
        runTest {
            geocodeClient.result = Result.Error(DataError.Network.NO_INTERNET)
            val viewModel = createViewModel()

            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)

            assertEquals(AddressSearchError.NETWORK_FAILURE, viewModel.state.value.errorType)
            assertTrue(
                viewModel.state.value.results
                    .isEmpty(),
            )
        }

    /** FR-005: a stale search response must never land after a newer query's results —
     *  [StandardTestDispatcher] (not this class's usual [UnconfinedTestDispatcher]) so both
     *  searches' suspension points are reached under this test's own explicit control, proving
     *  the first search is genuinely cancelled while in-flight rather than merely losing a race. */
    @Test
    fun `submitting a new search cancels a still-pending prior one`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val staleResponse = CompletableDeferred<Result<List<PlaceSearchResult>, DataError.Network>>()
            val freshResponse = CompletableDeferred<Result<List<PlaceSearchResult>, DataError.Network>>()
            geocodeClient.queuedResponses += staleResponse
            geocodeClient.queuedResponses += freshResponse
            val viewModel = createViewModel()

            viewModel.onAction(AddressSearchAction.OnQueryChange("stale query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            runCurrent()
            viewModel.onAction(AddressSearchAction.OnQueryChange("fresh query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            runCurrent()

            val freshResult = place(name = "Fresh")
            staleResponse.complete(Result.Success(listOf(place(name = "Stale"))))
            freshResponse.complete(Result.Success(listOf(freshResult)))
            advanceUntilIdle()

            assertEquals(listOf(freshResult), viewModel.state.value.results)
        }

    @Test
    fun `selecting a result when opened from Plan Route hands it off without a choice prompt`() =
        runTest {
            val result = place()
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel(sourceContext = SearchSourceContext.PLAN_ROUTE)
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(AddressSearchAction.OnSelectResult(0))

            assertEquals(result, pendingSearchWaypoint.pending)
            assertNull(viewModel.state.value.pendingChoiceResult)
            assertEquals(AddressSearchEvent.NavigateBack, eventDeferred.await())
        }

    @Test
    fun `selecting a result when opened from elsewhere prompts a teleport-or-favorite choice`() =
        runTest {
            val result = place()
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel(sourceContext = SearchSourceContext.TELEPORT_OR_FAVORITE)
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)

            viewModel.onAction(AddressSearchAction.OnSelectResult(0))

            assertEquals(result, viewModel.state.value.pendingChoiceResult)
            assertNull(pendingSearchWaypoint.pending)
        }

    @Test
    fun `selecting an out-of-range result index is a no-op`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.onAction(AddressSearchAction.OnSelectResult(0))

            assertNull(viewModel.state.value.pendingChoiceResult)
        }

    @Test
    fun `choosing teleport hands off the result's coordinates as a route point`() =
        runTest {
            val result = place(latitude = 10.0, longitude = 20.0)
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel()
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            viewModel.onAction(AddressSearchAction.OnSelectResult(0))
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(AddressSearchAction.OnChooseTeleport)

            assertEquals(NamedPlace(latitude = 10.0, longitude = 20.0, name = result.name), pendingTeleportTarget.pending)
            assertEquals(AddressSearchEvent.NavigateBack, eventDeferred.await())
        }

    @Test
    fun `teleporting to a result with a blank name hands off no name`() =
        runTest {
            geocodeClient.result = Result.Success(listOf(place(name = "  ", latitude = 10.0, longitude = 20.0)))
            val viewModel = createViewModel()
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            viewModel.onAction(AddressSearchAction.OnSelectResult(0))
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(AddressSearchAction.OnChooseTeleport)

            assertEquals(NamedPlace(latitude = 10.0, longitude = 20.0, name = null), pendingTeleportTarget.pending)
            assertEquals(AddressSearchEvent.NavigateBack, eventDeferred.await())
        }

    @Test
    fun `confirming a blank favorite name does not save it`() =
        runTest {
            val result = place()
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel()
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            viewModel.onAction(AddressSearchAction.OnSelectResult(0))
            viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)
            viewModel.onAction(AddressSearchAction.OnFavoriteNameChange("   "))

            viewModel.onAction(AddressSearchAction.OnConfirmFavoriteName)

            assertTrue(favoriteWaypointsRepository.added.isEmpty())
        }

    @Test
    fun `confirming a favorite name saves it under the result's coordinates`() =
        runTest {
            val result = place(latitude = 5.0, longitude = 6.0)
            geocodeClient.result = Result.Success(listOf(result))
            val viewModel = createViewModel()
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            viewModel.onAction(AddressSearchAction.OnSelectResult(0))
            viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)
            viewModel.onAction(AddressSearchAction.OnFavoriteNameChange("Home"))
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(AddressSearchAction.OnConfirmFavoriteName)

            assertEquals(listOf(Triple("Home", 5.0, 6.0)), favoriteWaypointsRepository.added)
            assertEquals(AddressSearchEvent.NavigateBack, eventDeferred.await())
        }

    @Test
    fun `a place that arrived from outside starts as a pending teleport-or-favorite choice`() {
        val viewModel = createViewModel(initialPlace = InitialPlace(52.52, 13.405, "Brandenburg Gate"))

        val choice = viewModel.state.value.pendingChoiceResult
        assertEquals("Brandenburg Gate", choice?.name)
        assertEquals(52.52, choice?.latitude)
        assertEquals(13.405, choice?.longitude)
    }

    @Test
    fun `saving an arrived place as a favorite starts from its name`() {
        val viewModel = createViewModel(initialPlace = InitialPlace(52.52, 13.405, "Brandenburg Gate"))

        viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)

        assertTrue(viewModel.state.value.isNamingFavorite)
        assertEquals("Brandenburg Gate", viewModel.state.value.favoriteNameInput)
    }

    @Test
    fun `an arrived place with no name has a blank name and the favorite name starts empty`() {
        val viewModel = createViewModel(initialPlace = InitialPlace(52.52, 13.405, name = null))

        assertEquals(
            "",
            viewModel.state.value.pendingChoiceResult
                ?.name,
        )
        viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)
        assertEquals("", viewModel.state.value.favoriteNameInput)
    }

    @Test
    fun `teleporting to an arrived place hands off its coordinates, like a tapped result`() =
        runTest {
            val viewModel = createViewModel(initialPlace = InitialPlace(52.52, 13.405, "Brandenburg Gate"))
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(AddressSearchAction.OnChooseTeleport)

            assertEquals(NamedPlace(latitude = 52.52, longitude = 13.405, name = "Brandenburg Gate"), pendingTeleportTarget.pending)
            assertEquals(AddressSearchEvent.NavigateBack, eventDeferred.await())
        }

    @Test
    fun `confirming the favorite name of an arrived place saves it under its coordinates`() {
        val viewModel = createViewModel(initialPlace = InitialPlace(52.52, 13.405, "Brandenburg Gate"))
        viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)

        viewModel.onAction(AddressSearchAction.OnConfirmFavoriteName)

        assertEquals(listOf(Triple("Brandenburg Gate", 52.52, 13.405)), favoriteWaypointsRepository.added)
    }

    @Test
    fun `an arrived place opened from Plan Route is handed off to route creation without a choice prompt`() =
        runTest {
            val viewModel = createViewModel(SearchSourceContext.PLAN_ROUTE, InitialPlace(52.52, 13.405, "Brandenburg Gate"))
            val eventDeferred = async { viewModel.events.first() }

            assertEquals("Brandenburg Gate", pendingSearchWaypoint.pending?.name)
            assertNull(viewModel.state.value.pendingChoiceResult)
            assertEquals(AddressSearchEvent.NavigateBack, eventDeferred.await())
        }

    @Test
    fun `without an arrived place nothing is pending`() {
        assertNull(createViewModel().state.value.pendingChoiceResult)
    }

    @Test
    fun `an initial query fills the field and is searched once on start`() {
        val viewModel = createViewModel(initialQuery = "Brandenburg Gate Pariser Platz")

        assertEquals("Brandenburg Gate Pariser Platz", viewModel.state.value.query)
        assertEquals(listOf("Brandenburg Gate Pariser Platz"), geocodeClient.queries)
        assertTrue(viewModel.state.value.hasSearched)
        assertEquals(1, viewModel.state.value.results.size)
    }

    @Test
    fun `an initial query with no place leaves no choice pending until a result is picked`() {
        val viewModel = createViewModel(initialQuery = "Brandenburg Gate")

        assertNull(viewModel.state.value.pendingChoiceResult)

        viewModel.onAction(AddressSearchAction.OnSelectResult(0))

        assertEquals(
            "Place",
            viewModel.state.value.pendingChoiceResult
                ?.name,
        )
    }

    @Test
    fun `without an initial query nothing is searched`() {
        val viewModel = createViewModel(initialQuery = null)

        assertEquals("", viewModel.state.value.query)
        assertEquals(emptyList<String>(), geocodeClient.queries)
        assertFalse(viewModel.state.value.hasSearched)
    }

    @Test
    fun `a blank initial query is not searched`() {
        val viewModel = createViewModel(initialQuery = "   ")

        assertEquals(emptyList<String>(), geocodeClient.queries)
        assertFalse(viewModel.state.value.hasSearched)
    }

    private val gate = NamedPlace(latitude = 52.5163, longitude = 13.3777, name = "Brandenburg Gate")
    private val unnamedSpot = NamedPlace(latitude = 52.1, longitude = 13.1, name = null)
    private val gateAsResult =
        PlaceSearchResult(name = "Brandenburg Gate", formattedAddress = "", latitude = 52.5163, longitude = 13.3777, importance = 0.0)

    @Test
    fun `the recent places in the state mirror the repository newest first`() {
        recentPlaces.record(unnamedSpot)
        recentPlaces.record(gate)

        val viewModel = createViewModel()

        assertEquals(listOf(gate, unnamedSpot), viewModel.state.value.recentPlaces)
    }

    @Test
    fun `the recent places in the state follow later changes of the repository`() {
        val viewModel = createViewModel()
        val before = viewModel.state.value.recentPlaces

        recentPlaces.record(gate)

        assertTrue(before.isEmpty())
        assertEquals(listOf(gate), viewModel.state.value.recentPlaces)
    }

    @Test
    fun `the recent list shows while the query is empty and there are entries`() {
        recentPlaces.record(gate)

        val viewModel = createViewModel()

        assertTrue(viewModel.state.value.showRecents)
    }

    @Test
    fun `the recent list shows while the query is only spaces`() {
        recentPlaces.record(gate)
        val viewModel = createViewModel()

        viewModel.onAction(AddressSearchAction.OnQueryChange("   "))

        assertTrue(viewModel.state.value.showRecents)
    }

    @Test
    fun `typing hides the recent list and emptying the field brings it back`() {
        recentPlaces.record(gate)
        val viewModel = createViewModel()

        viewModel.onAction(AddressSearchAction.OnQueryChange("b"))
        assertFalse(viewModel.state.value.showRecents)

        viewModel.onAction(AddressSearchAction.OnQueryChange(""))
        assertTrue(viewModel.state.value.showRecents)
    }

    @Test
    fun `the recent list never shows while there are no entries`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.state.value.showRecents)
    }

    @Test
    fun `selecting a recent entry from the teleport tab makes it the pending choice with its name and coordinates`() {
        recentPlaces.record(gate)
        val viewModel = createViewModel()

        viewModel.onAction(AddressSearchAction.OnSelectRecent(0))

        assertEquals(gateAsResult, viewModel.state.value.pendingChoiceResult)
    }

    @Test
    fun `selecting a recent entry with no name makes a pending choice with a blank name`() {
        recentPlaces.record(unnamedSpot)
        val viewModel = createViewModel()

        viewModel.onAction(AddressSearchAction.OnSelectRecent(0))

        val choice = viewModel.state.value.pendingChoiceResult
        assertEquals("", choice?.name)
        assertEquals(52.1, choice?.latitude)
    }

    @Test
    fun `selecting a recent entry from plan route hands the place to the add-waypoint hand-off and navigates back`() =
        runTest {
            recentPlaces.record(gate)
            val viewModel = createViewModel(sourceContext = SearchSourceContext.PLAN_ROUTE)
            val eventDeferred = async { viewModel.events.first() }

            viewModel.onAction(AddressSearchAction.OnSelectRecent(0))

            assertEquals(gateAsResult, pendingSearchWaypoint.pending)
            assertEquals(AddressSearchEvent.NavigateBack, eventDeferred.await())
        }

    @Test
    fun `a second tap on teleport while leaving navigates back only once`() =
        runTest {
            geocodeClient.result = Result.Success(listOf(place()))
            val viewModel = createViewModel()
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            viewModel.onAction(AddressSearchAction.OnSelectResult(0))
            val events = mutableListOf<AddressSearchEvent>()
            backgroundScope.launch { viewModel.events.toList(events) }
            runCurrent()

            viewModel.onAction(AddressSearchAction.OnChooseTeleport)
            viewModel.onAction(AddressSearchAction.OnChooseTeleport)
            runCurrent()

            assertEquals(listOf<AddressSearchEvent>(AddressSearchEvent.NavigateBack), events)
        }

    @Test
    fun `a second tap on confirm while leaving saves the favorite and navigates back only once`() =
        runTest {
            geocodeClient.result = Result.Success(listOf(place(latitude = 5.0, longitude = 6.0)))
            val viewModel = createViewModel()
            viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
            viewModel.onAction(AddressSearchAction.OnSubmit)
            viewModel.onAction(AddressSearchAction.OnSelectResult(0))
            viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)
            viewModel.onAction(AddressSearchAction.OnFavoriteNameChange("Home"))
            val events = mutableListOf<AddressSearchEvent>()
            backgroundScope.launch { viewModel.events.toList(events) }
            runCurrent()

            viewModel.onAction(AddressSearchAction.OnConfirmFavoriteName)
            viewModel.onAction(AddressSearchAction.OnConfirmFavoriteName)
            runCurrent()

            assertEquals(listOf(Triple("Home", 5.0, 6.0)), favoriteWaypointsRepository.added)
            assertEquals(listOf<AddressSearchEvent>(AddressSearchEvent.NavigateBack), events)
        }

    @Test
    fun `a second tap on a recent entry from plan route navigates back only once`() =
        runTest {
            recentPlaces.record(gate)
            val viewModel = createViewModel(sourceContext = SearchSourceContext.PLAN_ROUTE)
            val events = mutableListOf<AddressSearchEvent>()
            backgroundScope.launch { viewModel.events.toList(events) }
            runCurrent()

            viewModel.onAction(AddressSearchAction.OnSelectRecent(0))
            viewModel.onAction(AddressSearchAction.OnSelectRecent(0))
            runCurrent()

            assertEquals(listOf<AddressSearchEvent>(AddressSearchEvent.NavigateBack), events)
        }

    @Test
    fun `selecting a recent entry out of range does nothing`() {
        recentPlaces.record(gate)
        val viewModel = createViewModel()

        viewModel.onAction(AddressSearchAction.OnSelectRecent(5))

        assertNull(viewModel.state.value.pendingChoiceResult)
        assertNull(pendingSearchWaypoint.pending)
    }

    @Test
    fun `saving a favorite records the place under the favorite name`() {
        geocodeClient.result = Result.Success(listOf(place(name = "Found", latitude = 10.0, longitude = 20.0)))
        val viewModel = createViewModel()
        viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
        viewModel.onAction(AddressSearchAction.OnSubmit)
        viewModel.onAction(AddressSearchAction.OnSelectResult(0))
        viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)
        viewModel.onAction(AddressSearchAction.OnFavoriteNameChange("Home"))

        viewModel.onAction(AddressSearchAction.OnConfirmFavoriteName)

        assertEquals(listOf(NamedPlace(latitude = 10.0, longitude = 20.0, name = "Home")), recentPlaces.recorded)
    }

    @Test
    fun `canceling the favorite name step records nothing`() {
        val viewModel = createViewModel()
        viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
        viewModel.onAction(AddressSearchAction.OnSubmit)
        viewModel.onAction(AddressSearchAction.OnSelectResult(0))
        viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)

        viewModel.onAction(AddressSearchAction.OnDismissFavoriteName)

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `choosing teleport records nothing here because the teleport is recorded where it is carried out`() {
        val viewModel = createViewModel()
        viewModel.onAction(AddressSearchAction.OnQueryChange("query"))
        viewModel.onAction(AddressSearchAction.OnSubmit)
        viewModel.onAction(AddressSearchAction.OnSelectResult(0))

        viewModel.onAction(AddressSearchAction.OnChooseTeleport)

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `opening with an arrived place or text records nothing`() {
        createViewModel(initialPlace = InitialPlace(52.52, 13.405, "Brandenburg Gate"))
        createViewModel(initialQuery = "berlin")

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    private val bridge = NamedPlace(latitude = 52.52, longitude = 13.40, name = "Bridge")

    @Test
    fun `removing a recent entry removes only that entry and keeps the others in order`() {
        recentPlaces.record(unnamedSpot)
        recentPlaces.record(bridge)
        recentPlaces.record(gate)
        val viewModel = createViewModel()

        viewModel.onAction(AddressSearchAction.OnRemoveRecent(1))

        assertEquals(listOf(gate, unnamedSpot), viewModel.state.value.recentPlaces)
    }

    @Test
    fun `removing a recent entry out of range does nothing`() {
        recentPlaces.record(gate)
        val viewModel = createViewModel()

        viewModel.onAction(AddressSearchAction.OnRemoveRecent(3))

        assertEquals(listOf(gate), viewModel.state.value.recentPlaces)
    }

    @Test
    fun `removing the last recent entry hides the recent list`() {
        recentPlaces.record(gate)
        val viewModel = createViewModel()

        viewModel.onAction(AddressSearchAction.OnRemoveRecent(0))

        assertFalse(viewModel.state.value.showRecents)
    }

    @Test
    fun `asking to clear the list opens the confirmation and removes nothing`() {
        recentPlaces.record(gate)
        val viewModel = createViewModel()

        viewModel.onAction(AddressSearchAction.OnClearRecentsClick)

        assertTrue(viewModel.state.value.isClearRecentsConfirmOpen)
        assertEquals(listOf(gate), viewModel.state.value.recentPlaces)
    }

    @Test
    fun `confirming the clear empties the list and closes the confirmation`() {
        recentPlaces.record(gate)
        recentPlaces.record(bridge)
        val viewModel = createViewModel()
        viewModel.onAction(AddressSearchAction.OnClearRecentsClick)

        viewModel.onAction(AddressSearchAction.OnConfirmClearRecents)

        val recents = viewModel.state.value.recentPlaces
        assertTrue(recents.isEmpty())
        assertFalse(viewModel.state.value.isClearRecentsConfirmOpen)
    }

    @Test
    fun `dismissing the clear closes the confirmation and removes nothing`() {
        recentPlaces.record(gate)
        val viewModel = createViewModel()
        viewModel.onAction(AddressSearchAction.OnClearRecentsClick)

        viewModel.onAction(AddressSearchAction.OnDismissClearRecents)

        assertFalse(viewModel.state.value.isClearRecentsConfirmOpen)
        assertEquals(listOf(gate), viewModel.state.value.recentPlaces)
    }

    @Test
    fun `removing and clearing leave favorites and the pending hand-offs alone`() {
        recentPlaces.record(gate)
        recentPlaces.record(bridge)
        val viewModel = createViewModel()

        viewModel.onAction(AddressSearchAction.OnRemoveRecent(0))
        viewModel.onAction(AddressSearchAction.OnClearRecentsClick)
        viewModel.onAction(AddressSearchAction.OnConfirmClearRecents)

        assertTrue(favoriteWaypointsRepository.added.isEmpty())
        assertNull(pendingSearchWaypoint.pending)
        assertNull(pendingTeleportTarget.pending)
    }

    private val address = AddressLabel(full = "Pariser Platz 1, Mitte, Berlin", short = "Pariser Platz 1")

    private fun passTheLookupPause() {
        dispatcher.scheduler.advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS)
        dispatcher.scheduler.runCurrent()
    }

    private fun AddressSearchViewModel.chooseResultWithName(name: String) {
        geocodeClient.result = Result.Success(listOf(place(name = name, latitude = 10.0, longitude = 20.0)))
        onAction(AddressSearchAction.OnQueryChange("query"))
        onAction(AddressSearchAction.OnSubmit)
        onAction(AddressSearchAction.OnSelectResult(0))
    }

    @Test
    fun `choosing a result with no name shows its address after the pause`() {
        reverseGeocoder.answer = Result.Success(address)
        val viewModel = createViewModel()

        viewModel.chooseResultWithName("")
        assertNull(viewModel.state.value.choiceAddress)
        passTheLookupPause()

        assertEquals(address, viewModel.state.value.choiceAddress)
        assertEquals(listOf(10.0 to 20.0), reverseGeocoder.requests)
    }

    @Test
    fun `choosing a recent entry with no name shows its address after the pause`() {
        reverseGeocoder.answer = Result.Success(address)
        recentPlaces.record(unnamedSpot)
        val viewModel = createViewModel()

        viewModel.onAction(AddressSearchAction.OnSelectRecent(0))
        passTheLookupPause()

        assertEquals(address, viewModel.state.value.choiceAddress)
    }

    @Test
    fun `choosing a result that has a name makes no request`() {
        reverseGeocoder.answer = Result.Success(address)
        val viewModel = createViewModel()

        viewModel.chooseResultWithName("Eiffel Tower")
        passTheLookupPause()

        assertTrue(reverseGeocoder.requests.isEmpty())
        assertNull(viewModel.state.value.choiceAddress)
    }

    @Test
    fun `choosing a place with no name on plan route makes no request here`() {
        reverseGeocoder.answer = Result.Success(address)
        recentPlaces.record(unnamedSpot)
        val viewModel = createViewModel(sourceContext = SearchSourceContext.PLAN_ROUTE)

        viewModel.onAction(AddressSearchAction.OnSelectRecent(0))
        passTheLookupPause()

        assertTrue(reverseGeocoder.requests.isEmpty())
    }

    @Test
    fun `a failed lookup leaves no address and teleporting still works`() {
        reverseGeocoder.answer = Result.Error(DataError.Network.NO_INTERNET)
        val viewModel = createViewModel()
        viewModel.chooseResultWithName("")
        passTheLookupPause()

        viewModel.onAction(AddressSearchAction.OnChooseTeleport)

        assertNull(viewModel.state.value.choiceAddress)
        assertEquals(10.0, pendingTeleportTarget.pending?.latitude)
    }

    @Test
    fun `an answer of nothing found leaves no address`() {
        reverseGeocoder.answer = Result.Success(null)
        val viewModel = createViewModel()

        viewModel.chooseResultWithName("")
        passTheLookupPause()

        assertEquals(1, reverseGeocoder.requests.size)
        assertNull(viewModel.state.value.choiceAddress)
    }

    @Test
    fun `dismissing the choice before the address arrives discards it`() {
        reverseGeocoder.answer = Result.Success(address)
        reverseGeocoder.hold()
        val viewModel = createViewModel()
        viewModel.chooseResultWithName("")
        passTheLookupPause()

        viewModel.onAction(AddressSearchAction.OnDismissChoice)
        reverseGeocoder.release()
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.state.value.choiceAddress)
    }

    @Test
    fun `teleporting before the address arrives discards it`() {
        reverseGeocoder.answer = Result.Success(address)
        reverseGeocoder.hold()
        val viewModel = createViewModel()
        viewModel.chooseResultWithName("")
        passTheLookupPause()

        viewModel.onAction(AddressSearchAction.OnChooseTeleport)
        reverseGeocoder.release()
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.state.value.choiceAddress)
    }

    @Test
    fun `leaving the favorite name step keeps the address of the choice`() {
        reverseGeocoder.answer = Result.Success(address)
        val viewModel = createViewModel()
        viewModel.chooseResultWithName("")
        passTheLookupPause()
        viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)

        viewModel.onAction(AddressSearchAction.OnDismissFavoriteName)

        assertEquals(address, viewModel.state.value.choiceAddress)
    }

    @Test
    fun `saving a place with no name as a favorite starts the name as the short address`() {
        reverseGeocoder.answer = Result.Success(address)
        val viewModel = createViewModel()
        viewModel.chooseResultWithName("")
        passTheLookupPause()

        viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)

        assertEquals("Pariser Platz 1", viewModel.state.value.favoriteNameInput)
    }

    @Test
    fun `an address arriving during the name step fills an untouched field`() {
        reverseGeocoder.answer = Result.Success(address)
        val viewModel = createViewModel()
        viewModel.chooseResultWithName("")
        viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)
        assertEquals("", viewModel.state.value.favoriteNameInput)

        passTheLookupPause()

        assertEquals("Pariser Platz 1", viewModel.state.value.favoriteNameInput)
    }

    @Test
    fun `an address arriving after the user typed a name does not replace it`() {
        reverseGeocoder.answer = Result.Success(address)
        val viewModel = createViewModel()
        viewModel.chooseResultWithName("")
        viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)
        viewModel.onAction(AddressSearchAction.OnFavoriteNameChange("My spot"))

        passTheLookupPause()

        assertEquals("My spot", viewModel.state.value.favoriteNameInput)
    }

    @Test
    fun `clearing the favorite name keeps the address from filling it`() {
        reverseGeocoder.answer = Result.Success(address)
        val viewModel = createViewModel()
        viewModel.chooseResultWithName("")
        viewModel.onAction(AddressSearchAction.OnChooseSaveAsFavorite)
        viewModel.onAction(AddressSearchAction.OnFavoriteNameChange("x"))
        viewModel.onAction(AddressSearchAction.OnFavoriteNameChange(""))

        passTheLookupPause()

        assertEquals("", viewModel.state.value.favoriteNameInput)
    }
}
