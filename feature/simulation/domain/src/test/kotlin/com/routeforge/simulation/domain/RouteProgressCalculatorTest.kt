package com.routeforge.simulation.domain

import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.Route
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val metersPerDegreeLatitude = 6_371_000.0 * Math.toRadians(1.0)

class RouteProgressCalculatorTest {
    private val calculator = RouteProgressCalculator()

    private val northboundRoute =
        Route(
            points = emptyList(),
            geometry = listOf(LatLng(0.0, 0.0), LatLng(1.0, 0.0), LatLng(2.0, 0.0)),
            distanceMeters = metersPerDegreeLatitude * 2,
        )

    @Test
    fun `zero distance traveled stays at the route start facing north`() {
        val fix = calculator.interpolate(northboundRoute, distanceTraveledMeters = 0.0)

        assertEquals(0.0, fix.latitude, 0.000001)
        assertEquals(0.0, fix.longitude, 0.000001)
        assertEquals(0f, fix.bearingDegrees, 0.01f)
        assertTrue(!fix.isRouteExhausted)
    }

    @Test
    fun `distance halfway through the first segment interpolates the midpoint`() {
        val fix = calculator.interpolate(northboundRoute, distanceTraveledMeters = metersPerDegreeLatitude / 2)

        assertEquals(0.5, fix.latitude, 0.0001)
        assertEquals(0.0, fix.longitude, 0.000001)
        assertEquals(0f, fix.bearingDegrees, 0.01f)
        assertTrue(!fix.isRouteExhausted)
    }

    @Test
    fun `distance spanning into the second segment interpolates correctly`() {
        val fix = calculator.interpolate(northboundRoute, distanceTraveledMeters = metersPerDegreeLatitude * 1.5)

        assertEquals(1.5, fix.latitude, 0.0001)
        assertEquals(0.0, fix.longitude, 0.000001)
        assertTrue(!fix.isRouteExhausted)
    }

    @Test
    fun `distance exactly at the total route length reaches the end and is exhausted`() {
        val fix = calculator.interpolate(northboundRoute, distanceTraveledMeters = metersPerDegreeLatitude * 2)

        assertEquals(2.0, fix.latitude, 0.000001)
        assertEquals(0.0, fix.longitude, 0.000001)
        assertTrue(fix.isRouteExhausted)
    }

    @Test
    fun `distance beyond the total route length clamps to the final point and stays exhausted`() {
        val fix = calculator.interpolate(northboundRoute, distanceTraveledMeters = metersPerDegreeLatitude * 100)

        assertEquals(2.0, fix.latitude, 0.000001)
        assertEquals(0.0, fix.longitude, 0.000001)
        assertTrue(fix.isRouteExhausted)
    }

    @Test
    fun `a very short two-point route still interpolates and completes correctly`() {
        val shortRoute =
            Route(
                points = emptyList(),
                geometry = listOf(LatLng(0.0, 0.0), LatLng(0.0, 1.0)),
                distanceMeters = metersPerDegreeLatitude,
            )

        val midpoint = calculator.interpolate(shortRoute, distanceTraveledMeters = metersPerDegreeLatitude / 2)
        assertEquals(0.0, midpoint.latitude, 0.000001)
        assertEquals(0.5, midpoint.longitude, 0.0001)
        assertEquals(90f, midpoint.bearingDegrees, 0.01f)
        assertTrue(!midpoint.isRouteExhausted)

        val end = calculator.interpolate(shortRoute, distanceTraveledMeters = metersPerDegreeLatitude)
        assertEquals(0.0, end.latitude, 0.000001)
        assertEquals(1.0, end.longitude, 0.000001)
        assertTrue(end.isRouteExhausted)
    }

    @Test
    fun `projecting a point exactly on the route returns the matching cumulative distance`() {
        val distance = calculator.projectDistanceMeters(northboundRoute, latitude = 1.5, longitude = 0.0)

        assertEquals(metersPerDegreeLatitude * 1.5, distance, 1.0)
    }

    @Test
    fun `projecting a point off to the side snaps to the nearest point on the route`() {
        val distance = calculator.projectDistanceMeters(northboundRoute, latitude = 1.0, longitude = 0.5)

        assertEquals(metersPerDegreeLatitude, distance, 1.0)
    }

    @Test
    fun `projecting the exact start of the route returns zero`() {
        val distance = calculator.projectDistanceMeters(northboundRoute, latitude = 0.0, longitude = 0.0)

        assertEquals(0.0, distance, 1.0)
    }

    @Test
    fun `projecting the exact end of the route returns the total length`() {
        val distance = calculator.projectDistanceMeters(northboundRoute, latitude = 2.0, longitude = 0.0)

        assertEquals(metersPerDegreeLatitude * 2, distance, 1.0)
    }

    @Test
    fun `projecting onto a route with fewer than two points returns zero`() {
        val pointRoute = Route(points = emptyList(), geometry = listOf(LatLng(5.0, 5.0)), distanceMeters = 0.0)

        val distance = calculator.projectDistanceMeters(pointRoute, latitude = 10.0, longitude = 10.0)

        assertEquals(0.0, distance, 0.000001)
    }
}
