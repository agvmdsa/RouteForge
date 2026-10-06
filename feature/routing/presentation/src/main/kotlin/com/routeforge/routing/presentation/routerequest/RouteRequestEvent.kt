package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.model.Route
import com.routeforge.routing.domain.model.RouteFileFormat

sealed interface RouteRequestEvent {
    data class RouteComputed(
        val route: Route,
    ) : RouteRequestEvent

    class ExportReady(
        val bytes: ByteArray,
        val format: RouteFileFormat,
    ) : RouteRequestEvent

    data object NavigateToRegionCatalog : RouteRequestEvent

    data object NavigateToFavorites : RouteRequestEvent
}
