package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.DraftWaypointsHolder
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.coredomain.Result
import com.routeforge.routing.domain.FakeRegionCatalog
import com.routeforge.routing.domain.FakeRegionDownloader
import com.routeforge.routing.domain.FakeRegionUsageTracker
import com.routeforge.routing.domain.FakeStorageQuotaStore
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
        status = RegionStatus.NOT_DOWNLOADED,
    )

class DownloadRegionUseCaseTest {
    private val downloader = FakeRegionDownloader()
    private val catalog = FakeRegionCatalog(regions = listOf(region))
    private val regionUsageTracker = FakeRegionUsageTracker()
    private val storageQuotaStore = FakeStorageQuotaStore()

    private fun createUseCase(): DownloadRegionUseCase =
        DownloadRegionUseCase(
            regionDownloader = downloader,
            regionCatalog = catalog,
            recordRegionUsage = RecordRegionUsageUseCase(regionUsageTracker),
            enforceStorageQuota =
                EnforceStorageQuotaUseCase(
                    storageQuotaStore = storageQuotaStore,
                    regionCatalog = catalog,
                    regionUsageTracker = regionUsageTracker,
                    draftWaypointsHolder = DraftWaypointsHolder(),
                    lastComputedRouteHolder = LastComputedRouteHolder(),
                ),
        )

    @Test
    fun `a successful download records the region as used`() =
        runTest {
            createUseCase()(region.id) {}

            assertNotNull(regionUsageTracker.lastUsedAt(region.id))
        }

    @Test
    fun `a failed download does not record usage`() =
        runTest {
            downloader.result = Result.Error(DataError.Network.NO_INTERNET)

            createUseCase()(region.id) {}

            assertNull(regionUsageTracker.lastUsedAt(region.id))
        }

    @Test
    fun `a successful download triggers quota enforcement that can evict other regions`() =
        runTest {
            val justDownloaded = region.copy(status = RegionStatus.DOWNLOADED)
            val alreadyDownloaded =
                region.copy(id = "region-2", tileIds = listOf("region-2.rd5"), status = RegionStatus.DOWNLOADED)
            catalog.regions = listOf(justDownloaded, alreadyDownloaded)
            catalog.sizesOnDiskBytes = mapOf(justDownloaded.id to 200L, "region-2" to 900L)
            regionUsageTracker.recordUsed(setOf("region-2"), atMillis = 1L)
            storageQuotaStore.setMaxBytes(1_000L)
            downloader.progressSteps = listOf(1f)

            createUseCase()(justDownloaded.id) {}

            assertEquals(listOf(justDownloaded.id), catalog.regions.map { it.id })
        }

    @Test
    fun `an unknown region id returns not found without downloading`() =
        runTest {
            val result = createUseCase()("missing-region") {}

            assertEquals(Result.Error(DataError.Network.NOT_FOUND), result)
            assertNull(downloader.lastDownloadedRegion)
        }
}
