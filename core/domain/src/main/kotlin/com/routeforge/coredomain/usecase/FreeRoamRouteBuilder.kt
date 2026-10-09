package com.routeforge.coredomain.usecase

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
        val segmentDistances =
            geometry
                .zipWithNext { (lat1, lon1), (lat2, lon2) -> GeoMath.haversineMeters(lat1, lon1, lat2, lon2) }
        val distanceMeters = segmentDistances.sum()

        // geometry[i] == points[i] exactly for Free-roam, so the cumulative distance to
        // points[i] is just the running sum of the segments before it (spec 008, FR-012/FR-013).
        val waypointCumulativeDistances =
            if (points.isEmpty()) {
                emptyList()
            } else {
                val cumulative = mutableListOf(0.0)
                segmentDistances.forEach { cumulative.add(cumulative.last() + it) }
                cumulative
            }

        return Route(
            points = points,
            geometry = geometry,
            distanceMeters = distanceMeters,
            mode = RoutePlaybackMode.FREE_ROAM,
            waypointCumulativeDistances = waypointCumulativeDistances,
        )
    }
}
