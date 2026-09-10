package com.routeforge.simulation.domain

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
            geometry = listOf(0.0 to 0.0, 1.0 to 0.0, 2.0 to 0.0),
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
                geometry = listOf(0.0 to 0.0, 0.0 to 1.0),
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
}
