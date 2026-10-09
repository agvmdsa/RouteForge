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
}
