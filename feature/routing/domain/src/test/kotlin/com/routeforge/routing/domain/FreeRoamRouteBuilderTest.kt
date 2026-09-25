package com.routeforge.routing.domain

import com.routeforge.coredomain.GeoMath
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.model.RoutePlaybackMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FreeRoamRouteBuilderTest {
    private val builder = FreeRoamRouteBuilder()

    @Test
    fun `geometry follows the input points in order, unsnapped`() {
        val points =
            listOf(
                RoutePoint(latitude = 1.0, longitude = 1.0),
                RoutePoint(latitude = 2.0, longitude = 2.0, snappedLatitude = 99.0, snappedLongitude = 99.0),
                RoutePoint(latitude = 3.0, longitude = 3.0),
            )

        val route = builder.build(points)

        assertEquals(listOf(1.0 to 1.0, 2.0 to 2.0, 3.0 to 3.0), route.geometry)
        assertEquals(points, route.points)
    }

    @Test
    fun `mode is always FREE_ROAM`() {
        val points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0))

        assertEquals(RoutePlaybackMode.FREE_ROAM, builder.build(points).mode)
    }

    @Test
    fun `distance is the sum of haversine distances between consecutive points`() {
        val a = RoutePoint(latitude = 0.0, longitude = 0.0)
        val b = RoutePoint(latitude = 1.0, longitude = 0.0)
        val c = RoutePoint(latitude = 1.0, longitude = 1.0)

        val route = builder.build(listOf(a, b, c))

        val expected =
            GeoMath.haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude) +
                GeoMath.haversineMeters(b.latitude, b.longitude, c.latitude, c.longitude)
        assertEquals(expected, route.distanceMeters, 0.0001)
    }

    @Test
    fun `two identical consecutive points contribute a zero-length segment, not an error`() {
        val samePoint = RoutePoint(latitude = 5.0, longitude = 5.0)

        val route = builder.build(listOf(samePoint, samePoint))

        assertEquals(0.0, route.distanceMeters, 0.0001)
        assertEquals(listOf(5.0 to 5.0, 5.0 to 5.0), route.geometry)
    }
}
