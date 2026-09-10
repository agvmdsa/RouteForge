package com.routeforge.simulation.domain

import com.routeforge.coredomain.model.Route
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class InterpolatedFix(
    val latitude: Double,
    val longitude: Double,
    val bearingDegrees: Float,
    val isRouteExhausted: Boolean,
)

private const val EARTH_RADIUS_METERS = 6_371_000.0

class RouteProgressCalculator {
    fun interpolate(
        route: Route,
        distanceTraveledMeters: Double,
    ): InterpolatedFix {
        val geometry = route.geometry
        if (geometry.size < 2) {
            val point = geometry.firstOrNull() ?: (0.0 to 0.0)
            return InterpolatedFix(point.first, point.second, 0f, isRouteExhausted = true)
        }

        var remaining = distanceTraveledMeters
        val lastSegmentIndex = geometry.size - 2

        for (index in 0..lastSegmentIndex) {
            val (startLat, startLon) = geometry[index]
            val (endLat, endLon) = geometry[index + 1]
            val segmentLength = haversineMeters(startLat, startLon, endLat, endLon)
            val bearing = bearingDegrees(startLat, startLon, endLat, endLon)
            val isLastSegment = index == lastSegmentIndex

            if (remaining <= segmentLength || isLastSegment) {
                val isExhausted = isLastSegment && remaining >= segmentLength
                if (isExhausted) {
                    return InterpolatedFix(endLat, endLon, bearing, isRouteExhausted = true)
                }
                val fraction = if (segmentLength == 0.0) 0.0 else (remaining / segmentLength).coerceIn(0.0, 1.0)
                val latitude = startLat + (endLat - startLat) * fraction
                val longitude = startLon + (endLon - startLon) * fraction
                return InterpolatedFix(latitude, longitude, bearing, isRouteExhausted = false)
            }
            remaining -= segmentLength
        }

        val (lastLat, lastLon) = geometry.last()
        return InterpolatedFix(lastLat, lastLon, 0f, isRouteExhausted = true)
    }

    private fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    private fun bearingDegrees(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        val degrees = Math.toDegrees(theta)
        return ((degrees + 360.0) % 360.0).toFloat()
    }
}
