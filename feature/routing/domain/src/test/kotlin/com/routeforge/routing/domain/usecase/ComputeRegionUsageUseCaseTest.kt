package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.FavoriteRoute
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.routing.domain.FakeFavoriteRoutesRepository
import com.routeforge.coredomain.FakeRegionCatalog
import com.routeforge.coredomain.model.Region
import com.routeforge.coredomain.model.RegionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val region =
    Region(
        id = "region-1",
        displayName = "Region 1",
        minLatitude = 0.0,
        minLongitude = 0.0,
        maxLatitude = 1.0,
        maxLongitude = 1.0,
        tileIds = listOf("region-1.rd5"),
        approximateSizeBytes = 10L,
        status = RegionStatus.DOWNLOADED,
    )

private val pointInsideRegion = RoutePoint(latitude = 0.5, longitude = 0.5)
private val pointOutsideRegion = RoutePoint(latitude = 5.0, longitude = 5.0)

class ComputeRegionUsageUseCaseTest {
    private val catalog = FakeRegionCatalog(regions = listOf(region))
    private val draftWaypointsHolder = DraftWaypointsHolder()
    private val lastComputedRouteHolder = LastComputedRouteHolder()
    private val favoriteRoutesRepository = FakeFavoriteRoutesRepository()

    private fun createUseCase(): ComputeRegionUsageUseCase =
        ComputeRegionUsageUseCase(
            computeRequiredRegions = ComputeRequiredRegionsUseCase(catalog),
            draftWaypointsHolder = draftWaypointsHolder,
            lastComputedRouteHolder = lastComputedRouteHolder,
            favoriteRoutesRepository = favoriteRoutesRepository,
        )

    @Test
    fun `a region nothing depends on reports not in use`() {
        val usage = createUseCase()(region)

        assertFalse(usage.isInUse)
        assertFalse(usage.neededByDraft)
        assertFalse(usage.neededByActiveRoute)
        assertEquals(emptyList<String>(), usage.neededBySavedRouteNames)
    }

    @Test
    fun `a region touched by the current draft is reported as needed by the draft`() {
        draftWaypointsHolder.set(listOf(pointInsideRegion))

        val usage = createUseCase()(region)

        assertTrue(usage.isInUse)
        assertTrue(usage.neededByDraft)
        assertFalse(usage.neededByActiveRoute)
    }

    @Test
    fun `a region touched by the active route is reported as needed by the active route`() {
        lastComputedRouteHolder.set(Route(points = listOf(pointInsideRegion), geometry = emptyList(), distanceMeters = 0.0))

        val usage = createUseCase()(region)

        assertTrue(usage.isInUse)
        assertFalse(usage.neededByDraft)
        assertTrue(usage.neededByActiveRoute)
    }

    @Test
    fun `a region touched by one or more saved favorite routes surfaces their names`() {
        favoriteRoutesRepository.add(name = "Morning loop", points = listOf(pointInsideRegion))
        favoriteRoutesRepository.add(name = "Unrelated route", points = listOf(pointOutsideRegion))

        val usage = createUseCase()(region)

        assertTrue(usage.isInUse)
        assertEquals(listOf("Morning loop"), usage.neededBySavedRouteNames)
    }

    @Test
    fun `a region touched by a combination of draft, active route, and saved routes reports all of them`() {
        draftWaypointsHolder.set(listOf(pointInsideRegion))
        lastComputedRouteHolder.set(Route(points = listOf(pointInsideRegion), geometry = emptyList(), distanceMeters = 0.0))
        favoriteRoutesRepository.add(name = "Morning loop", points = listOf(pointInsideRegion))

        val usage = createUseCase()(region)

        assertTrue(usage.neededByDraft)
        assertTrue(usage.neededByActiveRoute)
        assertEquals(listOf("Morning loop"), usage.neededBySavedRouteNames)
    }
}
