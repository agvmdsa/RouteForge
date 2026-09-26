package com.routeforge.routing.domain.usecase

import com.routeforge.routing.domain.FakeRegionCatalog
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private fun region(
    id: String,
    status: RegionStatus,
) = Region(
    id = id,
    displayName = id,
    minLatitude = 0.0,
    minLongitude = 0.0,
    maxLatitude = 1.0,
    maxLongitude = 1.0,
    tileIds = listOf("$id.rd5"),
    approximateSizeBytes = 100L,
    status = status,
)

class GetStorageUsageSummaryUseCaseTest {
    @Test
    fun `sums real on-disk bytes across every downloaded region`() {
        val downloaded = region("a", RegionStatus.DOWNLOADED)
        val partial = region("b", RegionStatus.PARTIALLY_DOWNLOADED)
        val catalog =
            FakeRegionCatalog(
                regions = listOf(downloaded, partial),
                sizesOnDiskBytes = mapOf("a" to 300L, "b" to 150L),
            )

        assertEquals(450L, GetStorageUsageSummaryUseCase(catalog)())
    }

    @Test
    fun `excludes regions that are not downloaded or still downloading`() {
        val downloaded = region("a", RegionStatus.DOWNLOADED)
        val notDownloaded = region("b", RegionStatus.NOT_DOWNLOADED)
        val downloading = region("c", RegionStatus.DOWNLOADING)
        val catalog =
            FakeRegionCatalog(
                regions = listOf(downloaded, notDownloaded, downloading),
                sizesOnDiskBytes = mapOf("a" to 300L, "b" to 999L, "c" to 999L),
            )

        assertEquals(300L, GetStorageUsageSummaryUseCase(catalog)())
    }

    @Test
    fun `returns zero when nothing is downloaded`() {
        assertEquals(0L, GetStorageUsageSummaryUseCase(FakeRegionCatalog())())
    }
}
