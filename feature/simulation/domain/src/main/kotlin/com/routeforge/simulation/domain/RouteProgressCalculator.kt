package com.routeforge.simulation.domain

import com.routeforge.coredomain.GeoMath
import com.routeforge.coredomain.model.Route

data class InterpolatedFix(
    val latitude: Double,
    val longitude: Double,
    val bearingDegrees: Float,
    val isRouteExhausted: Boolean,
    /** Index of the geometry point immediately before this fix (or the last segment's start index
     *  once exhausted) — lets callers split the route into "already traveled" vs "remaining" for display. */
    val segmentIndex: Int,
)

class RouteProgressCalculator {
    fun interpolate(
        route: Route,
        distanceTraveledMeters: Double,
    ): InterpolatedFix {
        val geometry = route.geometry
        if (geometry.size < 2) {
            val point = geometry.firstOrNull() ?: (0.0 to 0.0)
            return InterpolatedFix(point.first, point.second, 0f, isRouteExhausted = true, segmentIndex = 0)
        }

        var remaining = distanceTraveledMeters
        val lastSegmentIndex = geometry.size - 2

        for (index in 0..lastSegmentIndex) {
            val (startLat, startLon) = geometry[index]
            val (endLat, endLon) = geometry[index + 1]
            val segmentLength = GeoMath.haversineMeters(startLat, startLon, endLat, endLon)
            val bearing = GeoMath.bearingDegrees(startLat, startLon, endLat, endLon)
            val isLastSegment = index == lastSegmentIndex

            if (remaining <= segmentLength || isLastSegment) {
                val isExhausted = isLastSegment && remaining >= segmentLength
                if (isExhausted) {
                    return InterpolatedFix(endLat, endLon, bearing, isRouteExhausted = true, segmentIndex = index)
                }
                val fraction = if (segmentLength == 0.0) 0.0 else (remaining / segmentLength).coerceIn(0.0, 1.0)
                val latitude = startLat + (endLat - startLat) * fraction
                val longitude = startLon + (endLon - startLon) * fraction
                return InterpolatedFix(latitude, longitude, bearing, isRouteExhausted = false, segmentIndex = index)
            }
            remaining -= segmentLength
        }

        val (lastLat, lastLon) = geometry.last()
        return InterpolatedFix(lastLat, lastLon, 0f, isRouteExhausted = true, segmentIndex = lastSegmentIndex)
    }
}
