package com.routeforge.routing.domain

import com.routeforge.coredomain.GeoMath
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint

/**
 * Builds a route whose playback geometry is exactly the input points, in order, with no road
 * snapping — always succeeds, unlike [ComputeRouteUseCase] (FR-006).
 */
class FreeRoamRouteBuilder {
    fun build(points: List<RoutePoint>): Route {
        val geometry = points.map { it.latitude to it.longitude }
        val distanceMeters =
            geometry
                .zipWithNext { (lat1, lon1), (lat2, lon2) -> GeoMath.haversineMeters(lat1, lon1, lat2, lon2) }
                .sum()

        return Route(
            points = points,
            geometry = geometry,
            distanceMeters = distanceMeters,
            mode = RoutePlaybackMode.FREE_ROAM,
        )
    }
}
