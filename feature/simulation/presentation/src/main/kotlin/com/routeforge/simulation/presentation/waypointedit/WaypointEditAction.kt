package com.routeforge.simulation.presentation.waypointedit

import com.routeforge.coredomain.model.Route

sealed interface WaypointEditAction {
    data class OnOpen(
        val route: Route,
        val distanceTraveledMeters: Double?,
    ) : WaypointEditAction

    data object OnClose : WaypointEditAction

    data class OnMapTap(
        val latitude: Double,
        val longitude: Double,
    ) : WaypointEditAction

    data object OnConfirmAdd : WaypointEditAction

    data object OnDismissAdd : WaypointEditAction

    data object OnToggleSaveAsFavorite : WaypointEditAction

    data class OnFavoriteNameChange(
        val value: String,
    ) : WaypointEditAction

    data class OnMarkerDragged(
        val index: Int,
        val latitude: Double,
        val longitude: Double,
    ) : WaypointEditAction

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
