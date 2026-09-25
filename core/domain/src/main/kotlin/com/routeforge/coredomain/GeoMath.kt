package com.routeforge.coredomain

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Shared spherical-earth math used by both real-road (Guided) and straight-line (Free-roam)
 * route geometry, and by playback position interpolation.
 */
object GeoMath {
    fun haversineMeters(
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

    fun bearingDegrees(
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

    /** Projects a point [distanceMeters] forward from (lat, lon) along [bearingDegrees]. Used for joystick movement. */
    fun destination(
        latitude: Double,
        longitude: Double,
        bearingDegrees: Float,
        distanceMeters: Double,
    ): Pair<Double, Double> {
        val angularDistance = distanceMeters / EARTH_RADIUS_METERS
        val bearingRadians = Math.toRadians(bearingDegrees.toDouble())
        val phi1 = Math.toRadians(latitude)
        val lambda1 = Math.toRadians(longitude)

        val phi2 = asin(sin(phi1) * cos(angularDistance) + cos(phi1) * sin(angularDistance) * cos(bearingRadians))
        val lambda2 =
            lambda1 +
                atan2(
                    sin(bearingRadians) * sin(angularDistance) * cos(phi1),
                    cos(angularDistance) - sin(phi1) * sin(phi2),
                )

        return Math.toDegrees(phi2) to Math.toDegrees(lambda2)
    }
}

