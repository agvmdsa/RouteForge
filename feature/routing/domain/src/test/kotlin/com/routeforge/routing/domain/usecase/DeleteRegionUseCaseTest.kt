package com.routeforge.routing.domain.usecase

import com.routeforge.routing.domain.FakeRegionCatalog
import com.routeforge.routing.domain.FakeRegionUsageTracker
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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

class DeleteRegionUseCaseTest {
    @Test
    fun `a successful delete clears the usage tracker entry`() {
        val catalog = FakeRegionCatalog(regions = listOf(region))
        val regionUsageTracker = FakeRegionUsageTracker(initialUsage = mapOf(region.id to 1L))
        val useCase = DeleteRegionUseCase(catalog, regionUsageTracker)

        val result = useCase(region)

        assertTrue(result)
        assertEquals(listOf(region.id), catalog.deletedRegionIds)
        assertNull(regionUsageTracker.lastUsedAt(region.id))
    }

    @Test
    fun `a failed delete does not clear the usage tracker entry`() {
        val catalog = FakeRegionCatalog(regions = listOf(region), regionIdsThatFailToDelete = setOf(region.id))
        val regionUsageTracker = FakeRegionUsageTracker(initialUsage = mapOf(region.id to 1L))
        val useCase = DeleteRegionUseCase(catalog, regionUsageTracker)

        val result = useCase(region)

        assertFalse(result)
        assertNotNull(regionUsageTracker.lastUsedAt(region.id))
    }
}
