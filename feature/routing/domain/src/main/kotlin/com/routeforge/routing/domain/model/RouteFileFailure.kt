package com.routeforge.routing.domain.model

import com.routeforge.coredomain.DomainError
import com.routeforge.coredomain.model.RoutePoint

private val LATITUDE_RANGE = -90.0..90.0
private val LONGITUDE_RANGE = -180.0..180.0

/** True when both coordinates are within valid lat/lon bounds — the shared check behind every
 *  [RouteFileCodec][com.routeforge.routing.domain.RouteFileCodec]'s [RouteFileFailure.CoordinateOutOfRange] gate. */
fun RoutePoint.hasValidCoordinates(): Boolean = latitude in LATITUDE_RANGE && longitude in LONGITUDE_RANGE

sealed interface RouteFileFailure : DomainError {
    data object TooFewWaypoints : RouteFileFailure

    data class CoordinateOutOfRange(
        val point: RoutePoint,
    ) : RouteFileFailure

    data object Malformed : RouteFileFailure
}
