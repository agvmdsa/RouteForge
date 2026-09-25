package com.routeforge.routing.domain.model

import com.routeforge.coredomain.Error
import com.routeforge.coredomain.model.RoutePoint

sealed interface RouteFileFailure : Error {
    data object TooFewWaypoints : RouteFileFailure

    data class CoordinateOutOfRange(
        val point: RoutePoint,
    ) : RouteFileFailure

    data object Malformed : RouteFileFailure
}
