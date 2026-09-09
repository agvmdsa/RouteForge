package com.routeforge.routing.presentation

import com.routeforge.routing.domain.model.Route

sealed interface RouteRequestEvent {
    data class RouteComputed(
        val route: Route,
    ) : RouteRequestEvent

    data object NavigateToRegionCatalog : RouteRequestEvent
}
