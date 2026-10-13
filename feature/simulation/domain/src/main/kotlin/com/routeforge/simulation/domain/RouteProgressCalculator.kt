package com.routeforge.simulation.domain

import com.routeforge.coredomain.GeoMath
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.Route
import kotlin.math.cos

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
            val point = geometry.firstOrNull() ?: LatLng(0.0, 0.0)
            return InterpolatedFix(point.latitude, point.longitude, 0f, isRouteExhausted = true, segmentIndex = 0)
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

    /** Finds the point on [route]'s geometry closest to (`latitude`, `longitude`) and returns the
     *  cumulative distance along that geometry to reach it — the inverse of [interpolate]. Used
     *  when swapping Guided ↔ Free-roam mid-session: the two geometries rarely have the same
     *  shape or total length, so re-using the old geometry's traveled-meters value on the new one
     *  can land well before or after where the user actually is. Projecting the current real
     *  position onto the new geometry instead keeps playback continuing from that exact point. */
    fun projectDistanceMeters(
        route: Route,
        latitude: Double,
        longitude: Double,
    ): Double {
        val geometry = route.geometry
        if (geometry.size < 2) return 0.0

        var cumulative = 0.0
        var bestDistanceAlong = 0.0
        var bestSquaredOffset = Double.MAX_VALUE

        for (index in 0 until geometry.size - 1) {
            val (startLat, startLon) = geometry[index]
            val (endLat, endLon) = geometry[index + 1]
            val segmentLength = GeoMath.haversineMeters(startLat, startLon, endLat, endLon)

            // Local flat approximation (degrees scaled by cos(latitude) for longitude) is accurate
            // enough over a single route segment's span to pick the closest segment and fraction
            // along it — the actual distance accumulated below still uses the real haversine
            // segment lengths, so this approximation only affects *which* point is picked, not the
            // reported distance.
            val lonScale = cos(Math.toRadians((startLat + endLat) / 2))
            val startX = startLon * lonScale
            val endX = endLon * lonScale
            val pointX = longitude * lonScale

            val deltaX = endX - startX
            val deltaY = endLat - startLat
            val segmentLengthSquared = deltaX * deltaX + deltaY * deltaY
            val fraction =
                if (segmentLengthSquared == 0.0) {
                    0.0
                } else {
                    (((pointX - startX) * deltaX + (latitude - startLat) * deltaY) / segmentLengthSquared)
                        .coerceIn(0.0, 1.0)
                }

            val closestX = startX + deltaX * fraction
            val closestY = startLat + deltaY * fraction
            val offsetX = pointX - closestX
            val offsetY = latitude - closestY
            val squaredOffset = offsetX * offsetX + offsetY * offsetY

            if (squaredOffset < bestSquaredOffset) {
                bestSquaredOffset = squaredOffset
                bestDistanceAlong = cumulative + segmentLength * fraction
            }

            cumulative += segmentLength
        }

        return bestDistanceAlong
    }
}
