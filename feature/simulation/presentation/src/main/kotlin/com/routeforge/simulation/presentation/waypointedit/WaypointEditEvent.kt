package com.routeforge.simulation.presentation.waypointedit

import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint

sealed interface WaypointEditEvent {
    data class Committed(
        val points: List<RoutePoint>,
        val activeRoute: Route,
    ) : WaypointEditEvent

    data object GoToDownloads : WaypointEditEvent

    /** FR-014: emitted alongside [GoToDownloads] only when an edit triggers the missing-regions
     *  warning while a simulation is actively running. */
    data object AutoPauseRequested : WaypointEditEvent

    /** Sent once, right after [Committed], only when the just-applied edit was the explicit
     *  "Apply changes" action — not every edit (delete/map-tap-add/marker-drag commit immediately
     *  and don't trigger this) — so the UI can close the sheet and show a success confirmation. */
    data object WaypointsUpdated : WaypointEditEvent
}
