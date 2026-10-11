package com.routeforge.coredomain

import com.routeforge.coredomain.model.RoutePoint

sealed interface RoutingFailure : DomainError {
    data class PointNotRoutable(
        val point: RoutePoint,
    ) : RoutingFailure

    data class PointOutsideAvailableCoverage(
        val point: RoutePoint,
    ) : RoutingFailure

    data object NoPathBetweenPoints : RoutingFailure
}
