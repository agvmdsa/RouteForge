package com.routeforge.routing.presentation

import com.routeforge.coredomain.model.Route

sealed interface RouteRequestEvent {
    data class RouteComputed(
        val route: Route,
    ) : RouteRequestEvent

    data object NavigateToRegionCatalog : RouteRequestEvent
}
