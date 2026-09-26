package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.DraftWaypointsHolder
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.FakeRegionCatalog
import com.routeforge.routing.domain.FakeRegionUsageTracker
import com.routeforge.routing.domain.FakeStorageQuotaStore
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun region(
    id: String,
    status: RegionStatus = RegionStatus.DOWNLOADED,
) = Region(
    id = id,
    displayName = id,
    minLatitude = id.hashCode().toDouble(),
    minLongitude = id.hashCode().toDouble(),
    maxLatitude = id.hashCode().toDouble() + 1.0,
    maxLongitude = id.hashCode().toDouble() + 1.0,
    tileIds = listOf("$id.rd5"),
    approximateSizeBytes = 100_000_000L,
    status = status,
)

private fun pointIn(region: Region) = RoutePoint(latitude = region.minLatitude + 0.5, longitude = region.minLongitude + 0.5)

class EnforceStorageQuotaUseCaseTest {
    private val storageQuotaStore = FakeStorageQuotaStore()
    private val regionUsageTracker = FakeRegionUsageTracker()
    private val draftWaypointsHolder = DraftWaypointsHolder()
    private val lastComputedRouteHolder = LastComputedRouteHolder()

    private fun createUseCase(catalog: FakeRegionCatalog) =
        EnforceStorageQuotaUseCase(
            storageQuotaStore = storageQuotaStore,
            regionCatalog = catalog,
            regionUsageTracker = regionUsageTracker,
            draftWaypointsHolder = draftWaypointsHolder,
            lastComputedRouteHolder = lastComputedRouteHolder,
        )

    @Test
    fun `does nothing when total usage is under budget`() {
        val regionA = region("a")
        val catalog = FakeRegionCatalog(regions = listOf(regionA), sizesOnDiskBytes = mapOf("a" to 100L))
        storageQuotaStore.setMaxBytes(1_000L)

        createUseCase(catalog)()

        assertEquals(listOf(regionA), catalog.regions)
    }

    @Test
    fun `is a no-op when the budget is Unlimited`() {
        val regionA = region("a")
        val catalog = FakeRegionCatalog(regions = listOf(regionA), sizesOnDiskBytes = mapOf("a" to 10_000L))
        storageQuotaStore.setMaxBytes(null)

        createUseCase(catalog)()

        assertEquals(listOf(regionA), catalog.regions)
    }

    @Test
    fun `evicts the least-recently-used region first until usage fits the budget`() {
        val regionA = region("a")
        val regionB = region("b")
        val catalog =
            FakeRegionCatalog(
                regions = listOf(regionA, regionB),
                sizesOnDiskBytes = mapOf("a" to 600L, "b" to 600L),
            )
        regionUsageTracker.recordUsed(setOf("a"), atMillis = 1L)
        regionUsageTracker.recordUsed(setOf("b"), atMillis = 2L)
        storageQuotaStore.setMaxBytes(1_000L)

        createUseCase(catalog)()

        assertEquals(listOf("b"), catalog.regions.map { it.id })
        assertEquals(listOf("a"), catalog.deletedRegionIds)
    }

    @Test
    fun `never evicts a region touching the current route draft`() {
        val regionA = region("a")
        val regionB = region("b")
        val catalog =
            FakeRegionCatalog(
                regions = listOf(regionA, regionB),
                sizesOnDiskBytes = mapOf("a" to 600L, "b" to 600L),
            )
        regionUsageTracker.recordUsed(setOf("a"), atMillis = 1L)
        regionUsageTracker.recordUsed(setOf("b"), atMillis = 2L)
        draftWaypointsHolder.set(listOf(pointIn(regionA)))
        storageQuotaStore.setMaxBytes(1_000L)

        createUseCase(catalog)()

        assertEquals(listOf("a"), catalog.regions.map { it.id })
        assertEquals(listOf("b"), catalog.deletedRegionIds)
    }

    @Test
    fun `never evicts a region touching the active loaded route`() {
        val regionA = region("a")
        val regionB = region("b")
        val catalog =
            FakeRegionCatalog(
                regions = listOf(regionA, regionB),
                sizesOnDiskBytes = mapOf("a" to 600L, "b" to 600L),
            )
        regionUsageTracker.recordUsed(setOf("a"), atMillis = 1L)
        regionUsageTracker.recordUsed(setOf("b"), atMillis = 2L)
        lastComputedRouteHolder.set(Route(points = listOf(pointIn(regionA)), geometry = emptyList(), distanceMeters = 0.0))
        storageQuotaStore.setMaxBytes(1_000L)

        createUseCase(catalog)()

        assertEquals(listOf("a"), catalog.regions.map { it.id })
    }

    @Test
    fun `stops once only protected regions remain, even if still over budget`() {
        val regionA = region("a")
        val catalog = FakeRegionCatalog(regions = listOf(regionA), sizesOnDiskBytes = mapOf("a" to 5_000L))
        draftWaypointsHolder.set(listOf(pointIn(regionA)))
        storageQuotaStore.setMaxBytes(1_000L)

        createUseCase(catalog)()

        assertEquals(listOf(regionA), catalog.regions)
        assertTrue(catalog.deletedRegionIds.isEmpty())
    }

    @Test
    fun `skips a region that fails to delete and continues to the next candidate`() {
        val regionA = region("a")
        val regionB = region("b")
        val catalog =
            FakeRegionCatalog(
                regions = listOf(regionA, regionB),
                sizesOnDiskBytes = mapOf("a" to 600L, "b" to 600L),
                regionIdsThatFailToDelete = setOf("a"),
            )
        regionUsageTracker.recordUsed(setOf("a"), atMillis = 1L)
        regionUsageTracker.recordUsed(setOf("b"), atMillis = 2L)
        storageQuotaStore.setMaxBytes(1_000L)

        createUseCase(catalog)()

        assertEquals(listOf("a"), catalog.regions.map { it.id })
        assertEquals(listOf("b"), catalog.deletedRegionIds)
    }
}
