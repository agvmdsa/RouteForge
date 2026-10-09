package com.routeforge.routing.presentation.routerequest.waypointedit

import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint

sealed interface WaypointEditEvent {
    /** Plan Route applies this via the existing `mutateDraft` invalidation rule (FR-018) — the
     *  already-computed [activeRoute] from this recompute is not reused, since Plan Route's
     *  "Finish Route" step always recomputes fresh, consistent with every other existing draft
     *  mutation. */
    data class Committed(
        val points: List<RoutePoint>,
        val activeRoute: Route,
    ) : WaypointEditEvent

    data object GoToDownloads : WaypointEditEvent
}
