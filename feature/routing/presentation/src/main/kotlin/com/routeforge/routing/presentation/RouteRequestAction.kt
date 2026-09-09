package com.routeforge.routing.presentation

sealed interface RouteRequestAction {
    data class OnStartLatitudeChange(
        val value: String,
    ) : RouteRequestAction

    data class OnStartLongitudeChange(
        val value: String,
    ) : RouteRequestAction

    data class OnEndLatitudeChange(
        val value: String,
    ) : RouteRequestAction

    data class OnEndLongitudeChange(
        val value: String,
    ) : RouteRequestAction

    data object OnAddWaypoint : RouteRequestAction

    data class OnRemoveWaypoint(
        val index: Int,
    ) : RouteRequestAction

    data class OnWaypointLatitudeChange(
        val index: Int,
        val value: String,
    ) : RouteRequestAction

    data class OnWaypointLongitudeChange(
        val index: Int,
        val value: String,
    ) : RouteRequestAction

    data object OnRequestRoute : RouteRequestAction

    data object OnOpenRegionCatalog : RouteRequestAction
}
