package com.routeforge.coredomain.usecase

import com.routeforge.coredomain.FakeRegionCatalog
import com.routeforge.coredomain.FakeRegionUsageTracker
import com.routeforge.coredomain.FakeRoutingEngine
import com.routeforge.coredomain.model.ModeAvailability
import com.routeforge.coredomain.model.Region
import com.routeforge.coredomain.model.RegionStatus
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class RecomputeRouteForBothModesUseCaseTest {
    private val routingEngine = FakeRoutingEngine()
    private val regionCatalog = FakeRegionCatalog()
    private val computeRoute = ComputeRouteUseCase(routingEngine, regionCatalog, RecordRegionUsageUseCase(FakeRegionUsageTracker()))
    private val freeRoamRouteBuilder = FreeRoamRouteBuilder()
    private val prepareRouteOptions = PrepareRouteOptionsUseCase(computeRoute, freeRoamRouteBuilder)
    private val computeRequiredRegions = ComputeRequiredRegionsUseCase(regionCatalog)
    private val useCase = RecomputeRouteForBothModesUseCase(computeRequiredRegions, prepareRouteOptions, computeRoute)

    private val start = RoutePoint(latitude = 1.0, longitude = 1.0)
    private val end = RoutePoint(latitude = 2.0, longitude = 2.0)
    private val points = listOf(start, end)

    private val guidedRoute =
        Route(points = points, geometry = listOf(1.0 to 1.0, 2.0 to 2.0), distanceMeters = 150.0)

    @Test
    fun `missing regions short-circuits before either mode is attempted`() {
        regionCatalog.regions =
            listOf(
                Region(
                    id = "r1",
                    displayName = "Region 1",
                    minLatitude = 0.0,
                    minLongitude = 0.0,
                    maxLatitude = 3.0,
                    maxLongitude = 3.0,
                    tileIds = listOf("t1"),
                    approximateSizeBytes = 100L,
                    status = RegionStatus.NOT_DOWNLOADED,
                ),
            )

        val result = useCase(points, RoutePlaybackMode.GUIDED)

        assertInstanceOf(RouteRecomputeResult.MissingRegions::class.java, result)
    }

    @Test
    fun `active mode failure returns ActiveModeFailed`() {
        routingEngine.computePathResult = null // Guided fails

        val result = useCase(points, RoutePlaybackMode.GUIDED)

        assertInstanceOf(RouteRecomputeResult.ActiveModeFailed::class.java, result)
    }

    @Test
    fun `active success and inactive failure returns Applied with UNAVAILABLE_PENDING_RETRY`() {
        routingEngine.computePathResult = null // Guided (inactive here) never succeeds, even on retry

        val result = useCase(points, RoutePlaybackMode.FREE_ROAM)

        val applied = result as RouteRecomputeResult.Applied
        assertEquals(ModeAvailability.UNAVAILABLE_PENDING_RETRY, applied.inactiveModeAvailability)
    }

    @Test
    fun `both modes succeeding returns Applied with AVAILABLE`() {
        routingEngine.computePathResult = guidedRoute

        val result = useCase(points, RoutePlaybackMode.GUIDED)

        val applied = result as RouteRecomputeResult.Applied
        assertEquals(ModeAvailability.AVAILABLE, applied.inactiveModeAvailability)
    }

    @Test
    fun `a transient inactive-mode failure that resolves within the immediate retries still returns AVAILABLE`() {
        // First attempt (inside prepareRouteOptions) fails; the use case's own retry loop's first
        // extra attempt succeeds — proving the retry loop runs rather than giving up immediately.
        routingEngine.computePathResults = listOf(null, guidedRoute)

        val result = useCase(points, RoutePlaybackMode.FREE_ROAM)

        val applied = result as RouteRecomputeResult.Applied
        assertEquals(ModeAvailability.AVAILABLE, applied.inactiveModeAvailability)
    }

    @Test
    fun `an inactive-mode failure that never resolves within the immediate retries returns UNAVAILABLE_PENDING_RETRY`() {
        routingEngine.computePathResults = listOf(null, null, null)

        val result = useCase(points, RoutePlaybackMode.FREE_ROAM)

        val applied = result as RouteRecomputeResult.Applied
        assertEquals(ModeAvailability.UNAVAILABLE_PENDING_RETRY, applied.inactiveModeAvailability)
    }
}
