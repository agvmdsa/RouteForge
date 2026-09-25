package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.FakeRegionCatalog
import com.routeforge.routing.domain.FakeRoutingEngine
import com.routeforge.routing.domain.FreeRoamRouteBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PrepareRouteOptionsUseCaseTest {
    private val routingEngine = FakeRoutingEngine()
    private val regionCatalog = FakeRegionCatalog()
    private val computeRoute = ComputeRouteUseCase(routingEngine, regionCatalog)
    private val freeRoamRouteBuilder = FreeRoamRouteBuilder()
    private val useCase = PrepareRouteOptionsUseCase(computeRoute, freeRoamRouteBuilder)

    private val start = RoutePoint(latitude = 1.0, longitude = 1.0)
    private val end = RoutePoint(latitude = 2.0, longitude = 2.0)

    @Test
    fun `both guided and free-roam are offered when the guided path is fully computable`() {
        routingEngine.computePathResult =
            com.routeforge.coredomain.model.Route(
                points = listOf(start, end),
                geometry = listOf(1.0 to 1.0, 2.0 to 2.0),
                distanceMeters = 150.0,
            )

        val options = useCase(listOf(start, end))

        assertEquals(RoutePlaybackMode.GUIDED, options.guided?.mode)
        assertEquals(RoutePlaybackMode.FREE_ROAM, options.freeRoam.mode)
    }

    @Test
    fun `only free-roam is offered when the point cannot be snapped anywhere`() {
        routingEngine.snapResult = { it } // never snaps -> PointOutsideAvailableCoverage

        val options = useCase(listOf(start, end))

        assertNull(options.guided)
        assertEquals(RoutePlaybackMode.FREE_ROAM, options.freeRoam.mode)
        assertEquals(listOf(start, end), options.freeRoam.points)
    }

    @Test
    fun `only free-roam is offered when there is no connecting path between snapped points`() {
        routingEngine.computePathResult = null

        val options = useCase(listOf(start, end))

        assertNull(options.guided)
        assertEquals(RoutePlaybackMode.FREE_ROAM, options.freeRoam.mode)
    }

    @Test
    fun `free-roam is never null even when guided succeeds`() {
        routingEngine.computePathResult =
            com.routeforge.coredomain.model.Route(
                points = listOf(start, end),
                geometry = listOf(1.0 to 1.0, 2.0 to 2.0),
                distanceMeters = 150.0,
            )

        val options = useCase(listOf(start, end))

        assertEquals(listOf(start, end), options.freeRoam.points)
    }
}
