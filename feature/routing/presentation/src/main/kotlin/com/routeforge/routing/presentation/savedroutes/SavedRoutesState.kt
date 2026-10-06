package com.routeforge.routing.presentation.savedroutes

import com.routeforge.coredomain.model.FavoriteRoute
import com.routeforge.routing.domain.model.RequiredRegionsSummary
import com.routeforge.routing.domain.model.RouteOptions

enum class SavedRoutesError { INVALID_NAME }

data class SavedRoutesState(
    val routes: List<FavoriteRoute> = emptyList(),
    val isComputingId: String? = null,
    val routeOptions: RouteOptions? = null,
    val resolvingRouteId: String? = null,
    val missingRegionsWarning: RequiredRegionsSummary? = null,
    val editingId: String? = null,
    val editNameInput: String = "",
    val editError: SavedRoutesError? = null,
    val pendingDeleteId: String? = null,
)
