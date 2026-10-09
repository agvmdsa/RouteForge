package com.routeforge.coredomain.usecase

import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ComputeEditableWaypointRangeUseCaseTest {
    private val useCase = ComputeEditableWaypointRangeUseCase()

    private val points =
        listOf(
            RoutePoint(latitude = 0.0, longitude = 0.0),
            RoutePoint(latitude = 1.0, longitude = 0.0),
            RoutePoint(latitude = 2.0, longitude = 0.0),
        )
    private val route =
        Route(
            points = points,
            geometry = points.map { it.latitude to it.longitude },
            distanceMeters = 200.0,
            waypointCumulativeDistances = listOf(0.0, 100.0, 200.0),
        )

    @Test
    fun `null distanceTraveledMeters means every waypoint is editable`() {
        val result = useCase(route, distanceTraveledMeters = null)

        assertEquals(0, result.firstEditableIndex)
    }

    @Test
    fun `empty waypointCumulativeDistances means every waypoint is editable`() {
        val routeWithoutBoundaryInfo = route.copy(waypointCumulativeDistances = emptyList())

        val result = useCase(routeWithoutBoundaryInfo, distanceTraveledMeters = 150.0)

        assertEquals(0, result.firstEditableIndex)
    }

    @Test
    fun `a position between two waypoints resolves to the next waypoint as the boundary`() {
        val result = useCase(route, distanceTraveledMeters = 50.0)

        assertEquals(1, result.firstEditableIndex)
    }

    @Test
    fun `a position exactly at a waypoint makes that waypoint itself the first editable one`() {
        val result = useCase(route, distanceTraveledMeters = 100.0)

        assertEquals(1, result.firstEditableIndex)
    }

    @Test
    fun `a position past every waypoint means nothing is editable`() {
        val result = useCase(route, distanceTraveledMeters = 500.0)

        assertEquals(points.size, result.firstEditableIndex)
    }
}
