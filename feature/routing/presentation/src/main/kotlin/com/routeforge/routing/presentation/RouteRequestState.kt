package com.routeforge.routing.presentation

import com.routeforge.coredomain.model.Route

data class RouteRequestState(
    val startLatitudeInput: String = "",
    val startLongitudeInput: String = "",
    val endLatitudeInput: String = "",
    val endLongitudeInput: String = "",
    val waypoints: List<WaypointInput> = emptyList(),
    val isComputing: Boolean = false,
    val route: Route? = null,
    val errorMessage: String? = null,
)

data class WaypointInput(
    val latitudeInput: String = "",
    val longitudeInput: String = "",
)
