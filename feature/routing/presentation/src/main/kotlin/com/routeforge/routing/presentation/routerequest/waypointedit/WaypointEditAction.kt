package com.routeforge.routing.presentation.routerequest.waypointedit

import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint

sealed interface WaypointEditAction {
    data class OnOpen(
        val points: List<RoutePoint>,
        val activeMode: RoutePlaybackMode,
    ) : WaypointEditAction

    data object OnClose : WaypointEditAction

    data class OnReorder(
        val fromIndex: Int,
        val toIndex: Int,
    ) : WaypointEditAction

    /** Commits whatever reorder is currently staged ([WaypointEditState.hasUnappliedReorder]) —
     *  the actual recompute only runs here, not on every intermediate swap during the drag. */
    data object OnApplyChanges : WaypointEditAction

    /** Discards the staged reorder, restoring the last successfully-committed order. */
    data object OnCancelChanges : WaypointEditAction

    data class OnDelete(
        val id: String,
    ) : WaypointEditAction

    data object OnDismissMissingRegions : WaypointEditAction

    data object OnGoToDownloads : WaypointEditAction

    data object OnContinueAnyway : WaypointEditAction
}
