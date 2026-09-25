package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.FakeRegionCatalog
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val northRegion =
    Region(
        id = "north",
        displayName = "North Region",
        minLatitude = 0.0,
        minLongitude = 0.0,
        maxLatitude = 10.0,
        maxLongitude = 10.0,
        tileIds = listOf("north.rd5"),
        approximateSizeBytes = 1_000L,
        status = RegionStatus.DOWNLOADED,
    )

private val southRegion =
    Region(
        id = "south",
        displayName = "South Region",
        minLatitude = -10.0,
        minLongitude = -10.0,
        maxLatitude = 0.0,
        maxLongitude = 0.0,
        tileIds = listOf("south.rd5"),
        approximateSizeBytes = 2_000L,
        status = RegionStatus.NOT_DOWNLOADED,
        missingTileCount = 1,
    )

class ComputeRequiredRegionsUseCaseTest {
    private val regionCatalog = FakeRegionCatalog(regions = listOf(northRegion, southRegion))
    private val useCase = ComputeRequiredRegionsUseCase(regionCatalog)

    @Test
    fun `waypoints in a single downloaded region report that region and no gaps`() {
        val summary = useCase(listOf(RoutePoint(latitude = 1.0, longitude = 1.0), RoutePoint(latitude = 2.0, longitude = 2.0)))

        assertEquals(listOf(northRegion), summary.regions)
        assertEquals(0, summary.uncoveredWaypointCount)
        assertTrue(summary.isFullyDownloaded)
    }

    @Test
    fun `waypoints spanning two regions report both distinct regions once each`() {
        val summary =
            useCase(
                listOf(
                    RoutePoint(latitude = 1.0, longitude = 1.0),
                    RoutePoint(latitude = -1.0, longitude = -1.0),
                    RoutePoint(latitude = 2.0, longitude = 2.0),
                ),
            )

        assertEquals(listOf(northRegion, southRegion), summary.regions)
        assertEquals(0, summary.uncoveredWaypointCount)
    }

    @Test
    fun `a region missing tiles makes the summary not fully downloaded and reports the missing size`() {
        val summary = useCase(listOf(RoutePoint(latitude = -1.0, longitude = -1.0)))

        assertFalse(summary.isFullyDownloaded)
        assertEquals(2_000L, summary.totalMissingBytes)
    }

    @Test
    fun `a waypoint outside every known region counts as uncovered but does not block guided readiness`() {
        val summary = useCase(listOf(RoutePoint(latitude = 100.0, longitude = 100.0)))

        assertEquals(1, summary.uncoveredWaypointCount)
        assertTrue(summary.regions.isEmpty())
        assertTrue(summary.isFullyDownloaded)
    }
}
