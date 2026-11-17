package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.AddressLookup
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.handoff.DraftWaypointsHandoffImpl
import com.routeforge.coredomain.handoff.PendingSearchWaypointHandoffImpl
import com.routeforge.coredomain.handoff.SelectedFavoriteWaypointHandoffImpl
import com.routeforge.coredomain.model.AddressLabel
import com.routeforge.coredomain.model.NamedPlace
import com.routeforge.coredomain.model.PlaceSearchResult
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.testing.FakeFavoriteWaypointsRepository
import com.routeforge.coredomain.testing.FakeRecentPlacesRepository
import com.routeforge.coredomain.testing.FakeRegionCatalog
import com.routeforge.coredomain.testing.FakeReverseGeocoder
import com.routeforge.coredomain.testing.FakeRoutingEngine
import com.routeforge.routing.domain.model.RouteDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PendingWaypointConfirmerTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val favoriteWaypointsRepository = FakeFavoriteWaypointsRepository()
    private val recentPlaces = FakeRecentPlacesRepository()
    private val reverseGeocoder = FakeReverseGeocoder()
    private val pendingSearchWaypoint = PendingSearchWaypointHandoffImpl()
    private val confirmer =
        PendingWaypointConfirmer(
            favoriteWaypointsRepository = favoriteWaypointsRepository,
            addressLookup = AddressLookup(reverseGeocoder),
            recentPlacesRepository = recentPlaces,
            pendingSearchWaypointReceiver = pendingSearchWaypoint,
            routeDraftEditor =
                RouteDraftEditor(
                    draftWaypointsWriter = DraftWaypointsHandoffImpl(),
                    selectedFavoriteWaypointReceiver = SelectedFavoriteWaypointHandoffImpl(),
                    recentPlacesRepository = recentPlaces,
                    routeModePreviewer =
                        newRouteModePreviewer(
                            FakeRoutingEngine(snappableLatitudes = emptySet()),
                            FakeRegionCatalog(),
                            dispatcher,
                        ),
                ),
        )

    @Test
    fun `confirming with nothing pending changes nothing`() {
        val state = MutableStateFlow(RouteRequestState())

        confirmer.confirmAddWaypoint(state, scope)

        assertEquals(emptyList<RoutePoint>(), state.value.draft.points)
    }

    @Test
    fun `confirming a save-as-favorite with a blank name changes nothing and keeps the question open`() {
        val state =
            MutableStateFlow(
                RouteRequestState(pendingAddLatitude = 1.0, pendingAddLongitude = 1.0, isSaveAsFavoriteChecked = true),
            )

        confirmer.confirmAddWaypoint(state, scope)

        assertEquals(emptyList<RoutePoint>(), state.value.draft.points)
        assertEquals(emptyList<Any>(), favoriteWaypointsRepository.observeFavorites().value)
        assertNotNull(state.value.pendingAddLatitude)
    }

    @Test
    fun `confirming adds the waypoint and clears the pending fields`() {
        val state = MutableStateFlow(RouteRequestState(pendingAddLatitude = 1.0, pendingAddLongitude = 2.0))

        confirmer.confirmAddWaypoint(state, scope)

        assertEquals(listOf(RoutePoint(1.0, 2.0)), state.value.draft.points)
        assertNull(state.value.pendingAddLatitude)
        assertNull(state.value.pendingAddLongitude)
        assertEquals(emptyList<Any>(), favoriteWaypointsRepository.observeFavorites().value)
    }

    @Test
    fun `confirming with save-as-favorite checked and a name also creates the favorite`() {
        val state =
            MutableStateFlow(
                RouteRequestState(
                    pendingAddLatitude = 1.0,
                    pendingAddLongitude = 2.0,
                    isSaveAsFavoriteChecked = true,
                    favoriteNameInput = "Home",
                ),
            )

        confirmer.confirmAddWaypoint(state, scope)

        val favorites = favoriteWaypointsRepository.observeFavorites().value
        assertEquals(1, favorites.size)
        assertEquals("Home", favorites[0].name)
        assertEquals(1.0, favorites[0].latitude)
        assertEquals(2.0, favorites[0].longitude)
        assertFalse(state.value.isSaveAsFavoriteChecked)
        assertEquals("", state.value.favoriteNameInput)
    }

    @Test
    fun `dismissing clears the pending fields and the favorite input`() {
        val state =
            MutableStateFlow(
                RouteRequestState(
                    draft = RouteDraft(points = listOf(RoutePoint(5.0, 5.0))),
                    pendingAddLatitude = 1.0,
                    pendingAddLongitude = 2.0,
                    pendingAddPlaceName = "Somewhere",
                    isSaveAsFavoriteChecked = true,
                    favoriteNameInput = "Home",
                ),
            )

        confirmer.dismissAddWaypoint(state)

        assertNull(state.value.pendingAddLatitude)
        assertNull(state.value.pendingAddLongitude)
        assertNull(state.value.pendingAddPlaceName)
        assertFalse(state.value.isSaveAsFavoriteChecked)
        assertEquals("", state.value.favoriteNameInput)
        assertEquals(listOf(RoutePoint(5.0, 5.0)), state.value.draft.points)
    }

    @Test
    fun `a place picked from address search becomes a named pending add and the holder is cleared`() {
        val state = MutableStateFlow(RouteRequestState())
        confirmer.startObservingPendingSearchWaypoint(state, scope)

        pendingSearchWaypoint.send(
            PlaceSearchResult(
                name = "Eiffel Tower",
                formattedAddress = "Paris, France",
                latitude = 48.8584,
                longitude = 2.2945,
                importance = 0.8,
            ),
        )

        assertEquals(48.8584, state.value.pendingAddLatitude)
        assertEquals(2.2945, state.value.pendingAddLongitude)
        assertEquals("Eiffel Tower", state.value.pendingAddPlaceName)
        assertEquals(emptyList<RoutePoint>(), state.value.draft.points)
        assertNull(pendingSearchWaypoint.pending)
    }

    @Test
    fun `a place with a blank name has no place name so the coordinates are shown`() {
        val state = MutableStateFlow(RouteRequestState())
        confirmer.startObservingPendingSearchWaypoint(state, scope)

        pendingSearchWaypoint.send(
            PlaceSearchResult(name = "", formattedAddress = "", latitude = 52.52, longitude = 13.405, importance = 0.0),
        )

        assertEquals(52.52, state.value.pendingAddLatitude)
        assertNull(state.value.pendingAddPlaceName)
    }

    private fun searchResult(name: String) =
        PlaceSearchResult(name = name, formattedAddress = "", latitude = 48.8584, longitude = 2.2945, importance = 0.8)

    @Test
    fun `confirming a place that arrived by name records it with its name`() {
        val state = MutableStateFlow(RouteRequestState())
        confirmer.startObservingPendingSearchWaypoint(state, scope)
        pendingSearchWaypoint.send(searchResult("Eiffel Tower"))

        confirmer.confirmAddWaypoint(state, scope)

        assertEquals(listOf(NamedPlace(latitude = 48.8584, longitude = 2.2945, name = "Eiffel Tower")), recentPlaces.recorded)
    }

    @Test
    fun `confirming a place that arrived with no name records it with no name`() {
        val state = MutableStateFlow(RouteRequestState())
        confirmer.startObservingPendingSearchWaypoint(state, scope)
        pendingSearchWaypoint.send(searchResult(""))

        confirmer.confirmAddWaypoint(state, scope)

        assertEquals(listOf(NamedPlace(latitude = 48.8584, longitude = 2.2945, name = null)), recentPlaces.recorded)
    }

    @Test
    fun `dismissing a pending add records nothing`() {
        val state = MutableStateFlow(RouteRequestState())
        confirmer.startObservingPendingSearchWaypoint(state, scope)
        pendingSearchWaypoint.send(searchResult("Eiffel Tower"))

        confirmer.dismissAddWaypoint(state)

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `confirming a waypoint tapped on the map records nothing`() {
        val state =
            MutableStateFlow(RouteRequestState(pendingAddLatitude = 1.0, pendingAddLongitude = 2.0, pendingAddFromMapTap = true))

        confirmer.confirmAddWaypoint(state, scope)

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `confirming a map tap saved as a favorite records the place under the favorite name`() {
        val state =
            MutableStateFlow(
                RouteRequestState(
                    pendingAddLatitude = 1.0,
                    pendingAddLongitude = 2.0,
                    pendingAddFromMapTap = true,
                    isSaveAsFavoriteChecked = true,
                    favoriteNameInput = "Spot",
                ),
            )

        confirmer.confirmAddWaypoint(state, scope)

        assertEquals(listOf(NamedPlace(latitude = 1.0, longitude = 2.0, name = "Spot")), recentPlaces.recorded)
    }

    @Test
    fun `confirming a named place saved as a favorite records it under the favorite name`() {
        val state = MutableStateFlow(RouteRequestState())
        confirmer.startObservingPendingSearchWaypoint(state, scope)
        pendingSearchWaypoint.send(searchResult("Eiffel Tower"))
        state.update { it.copy(isSaveAsFavoriteChecked = true, favoriteNameInput = "Paris") }

        confirmer.confirmAddWaypoint(state, scope)

        assertEquals(listOf(NamedPlace(latitude = 48.8584, longitude = 2.2945, name = "Paris")), recentPlaces.recorded)
    }

    @Test
    fun `a place that arrives after a map tap is recorded because the arrival clears the map tap mark`() {
        val state =
            MutableStateFlow(RouteRequestState(pendingAddLatitude = 1.0, pendingAddLongitude = 2.0, pendingAddFromMapTap = true))
        confirmer.startObservingPendingSearchWaypoint(state, scope)
        pendingSearchWaypoint.send(searchResult("Eiffel Tower"))

        confirmer.confirmAddWaypoint(state, scope)

        assertEquals(listOf(NamedPlace(latitude = 48.8584, longitude = 2.2945, name = "Eiffel Tower")), recentPlaces.recorded)
    }

    @Test
    fun `dismissing clears the map tap mark`() {
        val state = MutableStateFlow(RouteRequestState(pendingAddLatitude = 1.0, pendingAddLongitude = 2.0, pendingAddFromMapTap = true))

        confirmer.dismissAddWaypoint(state)

        assertFalse(state.value.pendingAddFromMapTap)
    }

    private val address = AddressLabel(full = "Pariser Platz 1, Mitte, Berlin", short = "Pariser Platz 1")

    private fun passTheLookupPause() {
        dispatcher.scheduler.advanceTimeBy(AddressLookup.DEBOUNCE_MILLIS)
        dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `a map tap sets the pending add and, after the pause, its address`() {
        reverseGeocoder.answer = Result.Success(address)
        val state = MutableStateFlow(RouteRequestState())

        confirmer.registerMapTap(latitude = 52.5, longitude = 13.4, state = state, scope = scope)
        assertNull(state.value.pendingAddAddress)
        passTheLookupPause()

        assertEquals(52.5, state.value.pendingAddLatitude)
        assertTrue(state.value.pendingAddFromMapTap)
        assertEquals(address, state.value.pendingAddAddress)
    }

    @Test
    fun `a place arriving with no name gets its address after the pause`() {
        reverseGeocoder.answer = Result.Success(address)
        val state = MutableStateFlow(RouteRequestState())
        confirmer.startObservingPendingSearchWaypoint(state, scope)

        pendingSearchWaypoint.send(searchResult(""))
        passTheLookupPause()

        assertEquals(address, state.value.pendingAddAddress)
    }

    @Test
    fun `a place arriving with a name makes no request`() {
        reverseGeocoder.answer = Result.Success(address)
        val state = MutableStateFlow(RouteRequestState())
        confirmer.startObservingPendingSearchWaypoint(state, scope)

        pendingSearchWaypoint.send(searchResult("Eiffel Tower"))
        passTheLookupPause()

        assertTrue(reverseGeocoder.requests.isEmpty())
        assertNull(state.value.pendingAddAddress)
    }

    @Test
    fun `a failed lookup leaves no address and the waypoint is still added at once`() {
        reverseGeocoder.answer = Result.Error(DataError.Network.NO_INTERNET)
        val state = MutableStateFlow(RouteRequestState())
        confirmer.registerMapTap(latitude = 1.0, longitude = 2.0, state = state, scope = scope)
        passTheLookupPause()

        confirmer.confirmAddWaypoint(state, scope)

        assertEquals(listOf(RoutePoint(1.0, 2.0)), state.value.draft.points)
        assertNull(state.value.pendingAddAddress)
    }

    @Test
    fun `an answer of nothing found leaves no address`() {
        reverseGeocoder.answer = Result.Success(null)
        val state = MutableStateFlow(RouteRequestState())

        confirmer.registerMapTap(latitude = 0.0, longitude = -30.0, state = state, scope = scope)
        passTheLookupPause()

        assertEquals(1, reverseGeocoder.requests.size)
        assertNull(state.value.pendingAddAddress)
    }

    @Test
    fun `confirming before the address arrives discards it`() {
        reverseGeocoder.answer = Result.Success(address)
        reverseGeocoder.hold()
        val state = MutableStateFlow(RouteRequestState())
        confirmer.registerMapTap(latitude = 1.0, longitude = 2.0, state = state, scope = scope)
        passTheLookupPause()

        confirmer.confirmAddWaypoint(state, scope)
        reverseGeocoder.release()
        dispatcher.scheduler.runCurrent()

        assertNull(state.value.pendingAddAddress)
    }

    @Test
    fun `dismissing before the address arrives discards it`() {
        reverseGeocoder.answer = Result.Success(address)
        reverseGeocoder.hold()
        val state = MutableStateFlow(RouteRequestState())
        confirmer.registerMapTap(latitude = 1.0, longitude = 2.0, state = state, scope = scope)
        passTheLookupPause()

        confirmer.dismissAddWaypoint(state)
        reverseGeocoder.release()
        dispatcher.scheduler.runCurrent()

        assertNull(state.value.pendingAddAddress)
    }

    @Test
    fun `a new map tap clears the earlier address until its own arrives`() {
        reverseGeocoder.answer = Result.Success(address)
        val state = MutableStateFlow(RouteRequestState())
        confirmer.registerMapTap(latitude = 1.0, longitude = 2.0, state = state, scope = scope)
        passTheLookupPause()
        assertEquals(address, state.value.pendingAddAddress)

        confirmer.registerMapTap(latitude = 3.0, longitude = 4.0, state = state, scope = scope)

        assertNull(state.value.pendingAddAddress)
    }

    @Test
    fun `an address arriving while the favorite name is untouched is offered as the name`() {
        reverseGeocoder.answer = Result.Success(address)
        val state = MutableStateFlow(RouteRequestState())

        confirmer.registerMapTap(latitude = 1.0, longitude = 2.0, state = state, scope = scope)
        passTheLookupPause()

        assertEquals("Pariser Platz 1", state.value.favoriteNameInput)
    }

    @Test
    fun `an address arriving after the user edited the favorite name does not replace it`() {
        reverseGeocoder.answer = Result.Success(address)
        val state = MutableStateFlow(RouteRequestState())
        confirmer.registerMapTap(latitude = 1.0, longitude = 2.0, state = state, scope = scope)
        state.update { it.copy(favoriteNameInput = "My spot", isFavoriteNameTouched = true) }

        passTheLookupPause()

        assertEquals("My spot", state.value.favoriteNameInput)
    }

    @Test
    fun `a new map tap resets the favorite name and the touched mark`() {
        val state = MutableStateFlow(RouteRequestState(favoriteNameInput = "My spot", isFavoriteNameTouched = true))

        confirmer.registerMapTap(latitude = 3.0, longitude = 4.0, state = state, scope = scope)

        assertEquals("", state.value.favoriteNameInput)
        assertFalse(state.value.isFavoriteNameTouched)
    }

    @Test
    fun `a new map tap lets its own address be offered as the name again`() {
        reverseGeocoder.answer = Result.Success(address)
        val state = MutableStateFlow(RouteRequestState(favoriteNameInput = "My spot", isFavoriteNameTouched = true))

        confirmer.registerMapTap(latitude = 3.0, longitude = 4.0, state = state, scope = scope)
        passTheLookupPause()

        assertEquals("Pariser Platz 1", state.value.favoriteNameInput)
    }

    @Test
    fun `a place arriving with no name resets the favorite name and the touched mark`() {
        val state = MutableStateFlow(RouteRequestState(favoriteNameInput = "My spot", isFavoriteNameTouched = true))
        confirmer.startObservingPendingSearchWaypoint(state, scope)

        pendingSearchWaypoint.send(searchResult(""))
        dispatcher.scheduler.runCurrent()

        assertEquals("", state.value.favoriteNameInput)
        assertFalse(state.value.isFavoriteNameTouched)
    }

    @Test
    fun `dismissing resets the favorite name and the touched mark`() {
        val state =
            MutableStateFlow(
                RouteRequestState(
                    pendingAddLatitude = 1.0,
                    pendingAddLongitude = 2.0,
                    pendingAddAddress = address,
                    favoriteNameInput = "My spot",
                    isFavoriteNameTouched = true,
                ),
            )

        confirmer.dismissAddWaypoint(state)

        assertEquals("", state.value.favoriteNameInput)
        assertFalse(state.value.isFavoriteNameTouched)
        assertNull(state.value.pendingAddAddress)
    }
}
