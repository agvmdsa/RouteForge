package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.Result
import com.routeforge.routing.domain.FakeRegionCatalog
import com.routeforge.routing.domain.FakeRoutingEngine
import com.routeforge.routing.domain.RoutingFailure
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import com.routeforge.routing.domain.model.Route
import com.routeforge.routing.domain.model.RoutePoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

private val bundledRegion =
    Region(
        id = "bundled",
        displayName = "Bundled Region",
        minLatitude = 0.0,
        minLongitude = 0.0,
        maxLatitude = 10.0,
        maxLongitude = 10.0,
        tileIds = listOf("E0_N0.rd5"),
        approximateSizeBytes = 1_000L,
        status = RegionStatus.BUNDLED,
    )

class ComputeRouteUseCaseTest {
    private val routingEngine = FakeRoutingEngine()
    private val regionCatalog = FakeRegionCatalog(regions = listOf(bundledRegion))
    private val useCase = ComputeRouteUseCase(routingEngine, regionCatalog)

    @Test
    fun `start and end that both snap and have a connecting path return a route`() {
        val start = RoutePoint(latitude = 1.0, longitude = 1.0)
        val end = RoutePoint(latitude = 2.0, longitude = 2.0)
        val expectedRoute =
            Route(
                points = listOf(start, end),
                geometry = listOf(1.0 to 1.0, 2.0 to 2.0),
                distanceMeters = 150.0,
            )
        routingEngine.computePathResult = expectedRoute

        val result = useCase(listOf(start, end))

        assertEquals(Result.Success(expectedRoute), result)
    }

    @Test
    fun `start, two waypoints, and end return a route visiting all four points in order`() {
        val start = RoutePoint(latitude = 1.0, longitude = 1.0)
        val waypoint1 = RoutePoint(latitude = 2.0, longitude = 2.0)
        val waypoint2 = RoutePoint(latitude = 3.0, longitude = 3.0)
        val end = RoutePoint(latitude = 4.0, longitude = 4.0)
        val requestedPoints = listOf(start, waypoint1, waypoint2, end)
        val expectedRoute =
            Route(
                points = requestedPoints,
                geometry = requestedPoints.map { it.latitude to it.longitude },
                distanceMeters = 500.0,
            )
        routingEngine.computePathResult = expectedRoute

        val result = useCase(requestedPoints)

        assertEquals(Result.Success(expectedRoute), result)
        assertEquals(requestedPoints, (result as Result.Success).data.points)
    }

    @Test
    fun `point inside a known region but unsnappable returns point not routable`() {
        val unroutablePoint = RoutePoint(latitude = 5.0, longitude = 5.0)
        val end = RoutePoint(latitude = 6.0, longitude = 6.0)
        routingEngine.snapResult = { point ->
            if (point == unroutablePoint) point else point.copy(snappedLatitude = point.latitude, snappedLongitude = point.longitude)
        }

        val result = useCase(listOf(unroutablePoint, end))

        assertEquals(Result.Error(RoutingFailure.PointNotRoutable(unroutablePoint)), result)
    }

    @Test
    fun `point outside every known region returns point outside available coverage`() {
        val outsidePoint = RoutePoint(latitude = 99.0, longitude = 99.0)
        val end = RoutePoint(latitude = 6.0, longitude = 6.0)
        routingEngine.snapResult = { point ->
            if (point == outsidePoint) point else point.copy(snappedLatitude = point.latitude, snappedLongitude = point.longitude)
        }

        val result = useCase(listOf(outsidePoint, end))

        assertEquals(Result.Error(RoutingFailure.PointOutsideAvailableCoverage(outsidePoint)), result)
    }

    @Test
    fun `two successfully snapped points with no connecting path return no path between points`() {
        val start = RoutePoint(latitude = 1.0, longitude = 1.0)
        val end = RoutePoint(latitude = 2.0, longitude = 2.0)
        routingEngine.computePathResult = null

        val result = useCase(listOf(start, end))

        assertInstanceOf(Result.Error::class.java, result)
        assertEquals(RoutingFailure.NoPathBetweenPoints, (result as Result.Error).error)
    }
}
