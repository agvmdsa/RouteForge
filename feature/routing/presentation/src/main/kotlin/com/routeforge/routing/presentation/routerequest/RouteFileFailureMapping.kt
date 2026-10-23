package com.routeforge.routing.presentation.routerequest

import com.routeforge.routing.domain.model.RouteFileFailure

internal fun RouteFileFailure.toRouteRequestError(): RouteRequestError =
    when (this) {
        RouteFileFailure.TooFewWaypoints -> RouteRequestError.IMPORT_TOO_FEW_WAYPOINTS
        is RouteFileFailure.CoordinateOutOfRange -> RouteRequestError.IMPORT_COORDINATE_OUT_OF_RANGE
        RouteFileFailure.Malformed -> RouteRequestError.IMPORT_MALFORMED
    }
