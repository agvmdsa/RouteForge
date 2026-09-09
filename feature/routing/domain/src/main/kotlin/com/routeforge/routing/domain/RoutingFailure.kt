package com.routeforge.routing.domain

import com.routeforge.coredomain.Error
import com.routeforge.routing.domain.model.RoutePoint

sealed interface RoutingFailure : Error {
    data class PointNotRoutable(
        val point: RoutePoint,
    ) : RoutingFailure

    data class PointOutsideAvailableCoverage(
        val point: RoutePoint,
    ) : RoutingFailure

    data object NoPathBetweenPoints : RoutingFailure
}
