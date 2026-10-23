package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.coredomain.holder.PendingSearchWaypointHolder
import com.routeforge.coredomain.holder.SelectedFavoriteWaypointHolder
import com.routeforge.coredomain.model.PlaceSearchResult
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.testing.FakeFavoriteWaypointsRepository
import com.routeforge.coredomain.testing.FakeRegionCatalog
import com.routeforge.coredomain.testing.FakeRoutingEngine
import com.routeforge.routing.domain.model.RouteDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PendingWaypointConfirmerTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val favoriteWaypointsRepository = FakeFavoriteWaypointsRepository()
    private val pendingSearchWaypointHolder = PendingSearchWaypointHolder()
    private val confirmer =
        PendingWaypointConfirmer(
            favoriteWaypointsRepository = favoriteWaypointsRepository,
            pendingSearchWaypointHolder = pendingSearchWaypointHolder,
            routeDraftEditor =
                RouteDraftEditor(
                    draftWaypointsHolder = DraftWaypointsHolder(),
                    selectedFavoriteWaypointHolder = SelectedFavoriteWaypointHolder(),
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

        pendingSearchWaypointHolder.set(
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
        assertNull(pendingSearchWaypointHolder.selected.value)
    }

    @Test
    fun `a place with a blank name has no place name so the coordinates are shown`() {
        val state = MutableStateFlow(RouteRequestState())
        confirmer.startObservingPendingSearchWaypoint(state, scope)

        pendingSearchWaypointHolder.set(
            PlaceSearchResult(name = "", formattedAddress = "", latitude = 52.52, longitude = 13.405, importance = 0.0),
        )

        assertEquals(52.52, state.value.pendingAddLatitude)
        assertNull(state.value.pendingAddPlaceName)
    }
}
