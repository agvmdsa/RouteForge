package com.routeforge.coredomain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private const val EARTH_RADIUS_METERS = 6_371_000.0
private val metersPerDegreeAtEquator = EARTH_RADIUS_METERS * Math.toRadians(1.0)

class GeoMathTest {
    @Test
    fun `distance between identical points is zero`() {
        assertEquals(0.0, GeoMath.haversineMeters(10.0, 20.0, 10.0, 20.0), 0.0001)
    }

    @Test
    fun `distance one degree of latitude apart matches the spherical-earth approximation`() {
        val distance = GeoMath.haversineMeters(0.0, 0.0, 1.0, 0.0)

        assertEquals(metersPerDegreeAtEquator, distance, 1.0)
    }

    @Test
    fun `distance is symmetric regardless of argument order`() {
        val forward = GeoMath.haversineMeters(-8.05, -34.88, -8.10, -34.90)
        val backward = GeoMath.haversineMeters(-8.10, -34.90, -8.05, -34.88)

        assertEquals(forward, backward, 0.0001)
    }

    @Test
    fun `bearing due north is zero degrees`() {
        val bearing = GeoMath.bearingDegrees(0.0, 0.0, 1.0, 0.0)

        assertEquals(0f, bearing, 0.01f)
    }

    @Test
    fun `bearing due east is ninety degrees`() {
        val bearing = GeoMath.bearingDegrees(0.0, 0.0, 0.0, 1.0)

        assertEquals(90f, bearing, 0.01f)
    }

    @Test
    fun `bearing due south is one hundred eighty degrees`() {
        val bearing = GeoMath.bearingDegrees(1.0, 0.0, 0.0, 0.0)

        assertEquals(180f, bearing, 0.01f)
    }

    @Test
    fun `bearing due west is two hundred seventy degrees`() {
        val bearing = GeoMath.bearingDegrees(0.0, 1.0, 0.0, 0.0)

        assertEquals(270f, bearing, 0.01f)
    }

    @Test
    fun `destination due north advances latitude and keeps longitude unchanged`() {
        val (latitude, longitude) = GeoMath.destination(0.0, 0.0, bearingDegrees = 0f, distanceMeters = metersPerDegreeAtEquator)

        assertEquals(1.0, latitude, 0.001)
        assertEquals(0.0, longitude, 0.001)
    }

    @Test
    fun `destination due east advances longitude and keeps latitude unchanged`() {
        val (latitude, longitude) = GeoMath.destination(0.0, 0.0, bearingDegrees = 90f, distanceMeters = metersPerDegreeAtEquator)

        assertEquals(0.0, latitude, 0.001)
        assertEquals(1.0, longitude, 0.001)
    }

    @Test
    fun `destination with zero distance returns the same point`() {
        val (latitude, longitude) = GeoMath.destination(10.0, 20.0, bearingDegrees = 45f, distanceMeters = 0.0)

        assertEquals(10.0, latitude, 0.0001)
        assertEquals(20.0, longitude, 0.0001)
    }
}
