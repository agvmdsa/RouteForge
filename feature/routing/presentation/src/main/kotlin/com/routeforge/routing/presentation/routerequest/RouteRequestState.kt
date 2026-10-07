package com.routeforge.routing.presentation.routerequest

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
    val pendingAddLatitude: Double? = null,
    val pendingAddLongitude: Double? = null,
    val isSaveAsFavoriteChecked: Boolean = false,
    val favoriteNameInput: String = "",
    val isSaveRouteSheetOpen: Boolean = false,
    val routeNameInput: String = "",
    val pendingGoToSimulateConfirmation: Boolean = false,
)
