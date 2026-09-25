package com.routeforge.routing.presentation

import com.routeforge.coredomain.model.RealLocation
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.routing.domain.model.RequiredRegionsSummary
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteOptions

data class RouteRequestState(
    val draft: RouteDraft = RouteDraft(),
    val editingIndex: Int? = null,
    val editLatitudeInput: String = "",
    val editLongitudeInput: String = "",
    val isComputing: Boolean = false,
    val routeOptions: RouteOptions? = null,
    val chosenMode: RoutePlaybackMode? = null,
    val route: Route? = null,
    val errorType: RouteRequestError? = null,
    val lastKnownLocation: RealLocation? = null,
    val missingRegionsWarning: RequiredRegionsSummary? = null,
)
