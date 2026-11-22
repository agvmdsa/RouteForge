package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.handoff.DraftWaypointsHandoffImpl
import com.routeforge.coredomain.handoff.SelectedFavoriteWaypointHandoffImpl
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.NamedPlace
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.testing.FakeRecentPlacesRepository
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RouteDraftEditorTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val routingEngine = FakeRoutingEngine(snappableLatitudes = emptySet())
    private val draftWaypoints = DraftWaypointsHandoffImpl()
    private val selectedFavoriteWaypoint = SelectedFavoriteWaypointHandoffImpl()
    private val recentPlaces = FakeRecentPlacesRepository()
    private val editor =
        RouteDraftEditor(
            draftWaypointsWriter = draftWaypoints,
            selectedFavoriteWaypointReceiver = selectedFavoriteWaypoint,
            recentPlacesRepository = recentPlaces,
            routeModePreviewer = newRouteModePreviewer(routingEngine, FakeRegionCatalog(), dispatcher),
        )

    private val twoPoints = RouteDraft(points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)))
    private val someRoute =
        Route(
            points = twoPoints.points,
            geometry = listOf(LatLng(1.0, 1.0), LatLng(2.0, 2.0)),
            distanceMeters = 100.0,
        )

    @Test
    fun `a draft change clears the route, the chosen mode and the error and writes the draft holder`() {
        val state =
            MutableStateFlow(
                RouteRequestState(
                    draft = twoPoints,
                    route = someRoute,
                    chosenMode = RoutePlaybackMode.GUIDED,
                    errorType = RouteRequestError.TOO_FEW_WAYPOINTS_TO_PLAY,
                ),
            )

        editor.mutateDraft(state, scope) { it.add(RoutePoint(3.0, 3.0)) }

        assertNull(state.value.route)
        assertNull(state.value.chosenMode)
        assertNull(state.value.errorType)
        assertEquals(3, state.value.draft.points.size)
        assertEquals(state.value.draft.points, draftWaypoints.points.value)
    }

    @Test
    fun `a draft change recomputes a guided preview that was showing`() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0, 3.0)
        routingEngine.computePathResult = someRoute
        val state =
            MutableStateFlow(
                RouteRequestState(draft = twoPoints, previewMode = RoutePlaybackMode.GUIDED, previewRoute = someRoute),
            )

        editor.mutateDraft(state, scope) { it.add(RoutePoint(3.0, 3.0)) }

        assertEquals(RoutePlaybackMode.GUIDED, state.value.previewMode)
        assertNotNull(state.value.previewRoute)
        assertFalse(state.value.isComputingPreview)
    }

    @Test
    fun `a draft change drops a preview that was not guided`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints, previewRoute = someRoute))

        editor.mutateDraft(state, scope) { it.add(RoutePoint(3.0, 3.0)) }

        assertNull(state.value.previewMode)
        assertNull(state.value.previewRoute)
    }

    @Test
    fun `opening the edit dialog prefills the coordinates of that waypoint`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))

        editor.openEditDialog(1, state)

        assertEquals(1, state.value.editingIndex)
        assertEquals("2.0", state.value.editLatitudeInput)
        assertEquals("2.0", state.value.editLongitudeInput)
    }

    @Test
    fun `opening the edit dialog for a waypoint that does not exist changes nothing`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))

        editor.openEditDialog(5, state)

        assertNull(state.value.editingIndex)
    }

    @Test
    fun `confirming an edit with coordinates that are not numbers shows the error and keeps the dialog open`() {
        val state =
            MutableStateFlow(
                RouteRequestState(draft = twoPoints, editingIndex = 0, editLatitudeInput = "abc", editLongitudeInput = "2.0"),
            )

        editor.confirmEdit(state, scope)

        assertEquals(RouteRequestError.INVALID_EDIT_COORDINATES, state.value.errorType)
        assertEquals(0, state.value.editingIndex)
        assertEquals(twoPoints.points, state.value.draft.points)
    }

    @Test
    fun `confirming an edit with a coordinate that is not finite or out of range shows the error and keeps the dialog open`() {
        val rejected =
            listOf(
                "NaN" to "2.0",
                "Infinity" to "2.0",
                "1e999" to "2.0",
                "95" to "2.0",
                "1.0" to "NaN",
                "1.0" to "-Infinity",
                "1.0" to "200",
            )

        for ((latitude, longitude) in rejected) {
            val state =
                MutableStateFlow(
                    RouteRequestState(draft = twoPoints, editingIndex = 0, editLatitudeInput = latitude, editLongitudeInput = longitude),
                )

            editor.confirmEdit(state, scope)

            assertEquals(RouteRequestError.INVALID_EDIT_COORDINATES, state.value.errorType, "$latitude, $longitude")
            assertEquals(0, state.value.editingIndex, "$latitude, $longitude")
            assertEquals(twoPoints.points, state.value.draft.points, "$latitude, $longitude")
        }
    }

    @Test
    fun `confirming an edit replaces the waypoint and closes the dialog`() {
        val state =
            MutableStateFlow(
                RouteRequestState(draft = twoPoints, editingIndex = 0, editLatitudeInput = "5.5", editLongitudeInput = "6.5"),
            )

        editor.confirmEdit(state, scope)

        assertEquals(RoutePoint(5.5, 6.5), state.value.draft.points[0])
        assertNull(state.value.editingIndex)
        assertEquals(state.value.draft.points, draftWaypoints.points.value)
    }

    @Test
    fun `confirming an edit accepts a decimal comma`() {
        val state =
            MutableStateFlow(
                RouteRequestState(draft = twoPoints, editingIndex = 0, editLatitudeInput = "-23,55", editLongitudeInput = " 6,5 "),
            )

        editor.confirmEdit(state, scope)

        assertEquals(RoutePoint(-23.55, 6.5), state.value.draft.points[0])
        assertNull(state.value.editingIndex)
    }

    @Test
    fun `confirming or deleting with no waypoint being edited changes nothing`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))

        editor.confirmEdit(state, scope)
        editor.deleteEditingWaypoint(state, scope)

        assertEquals(twoPoints.points, state.value.draft.points)
        assertNull(state.value.errorType)
    }

    @Test
    fun `deleting the waypoint being edited removes it and closes the dialog`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints, editingIndex = 0))

        editor.deleteEditingWaypoint(state, scope)

        assertEquals(listOf(RoutePoint(2.0, 2.0)), state.value.draft.points)
        assertNull(state.value.editingIndex)
    }

    @Test
    fun `a favorite selected elsewhere is added to the draft once and cleared`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))
        editor.startObservingSelectedFavoriteWaypoint(state, scope)

        selectedFavoriteWaypoint.send(NamedPlace(9.0, 9.0, "Spot"))

        assertEquals(twoPoints.points + RoutePoint(9.0, 9.0), state.value.draft.points)
        assertNull(selectedFavoriteWaypoint.pending)
    }

    @Test
    fun `a favorite selected elsewhere is recorded with its name`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))
        editor.startObservingSelectedFavoriteWaypoint(state, scope)

        selectedFavoriteWaypoint.send(NamedPlace(9.0, 9.0, "Spot"))

        assertEquals(listOf(NamedPlace(9.0, 9.0, "Spot")), recentPlaces.recorded)
    }

    @Test
    fun `changing the draft records nothing`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))

        editor.mutateDraft(state, scope) { it.add(RoutePoint(3.0, 3.0)) }

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `confirming an edit records nothing`() {
        val state =
            MutableStateFlow(
                RouteRequestState(draft = twoPoints, editingIndex = 0, editLatitudeInput = "5.5", editLongitudeInput = "6.5"),
            )

        editor.confirmEdit(state, scope)

        assertTrue(recentPlaces.recorded.isEmpty())
    }

    @Test
    fun `deleting the waypoint being edited records nothing`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints, editingIndex = 0))

        editor.deleteEditingWaypoint(state, scope)

        assertTrue(recentPlaces.recorded.isEmpty())
    }
}
