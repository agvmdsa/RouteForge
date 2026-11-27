package com.routeforge.routing.data

import com.routeforge.coredata.ForegroundServiceController
import com.routeforge.coredata.NoOpForegroundServiceController
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.EmptyResult
import com.routeforge.coredomain.RegionCatalog
import com.routeforge.coredomain.RegionDeletionResult
import com.routeforge.coredomain.RegionUsageTracker
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.handoff.DraftWaypointsHandoffImpl
import com.routeforge.coredomain.handoff.LastComputedRouteHandoffImpl
import com.routeforge.coredomain.model.Region
import com.routeforge.coredomain.model.RegionStatus
import com.routeforge.coredomain.usecase.RecordRegionUsageUseCase
import com.routeforge.routing.domain.RegionDownloadEvent
import com.routeforge.routing.domain.RegionDownloader
import com.routeforge.routing.domain.testing.FakeRegionDownloader
import com.routeforge.routing.domain.testing.FakeStorageQuotaStore
import com.routeforge.routing.domain.usecase.DownloadRegionUseCase
import com.routeforge.routing.domain.usecase.EnforceStorageQuotaUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
        status = RegionStatus.NOT_DOWNLOADED,
    )

private val otherRegion = region.copy(id = "region-2", tileIds = listOf("region-2.rd5"))

private class FakeRegionCatalog : RegionCatalog {
    val deleteRegionCalls = mutableListOf<Region>()

    override fun listRegions(): List<Region> = emptyList()

    override fun regionContaining(
        latitude: Double,
        longitude: Double,
    ): Region? = null

    override fun regionById(id: String): Region? = listOf(region, otherRegion).firstOrNull { it.id == id }

    override fun actualSizeOnDiskBytes(region: Region): Long = 0L

    override fun deleteRegion(region: Region): RegionDeletionResult {
        deleteRegionCalls.add(region)
        return RegionDeletionResult.FULLY_DELETED
    }
}

private class CountingForegroundService : ForegroundServiceController {
    var starts = 0
    var stops = 0

    override fun start() {
        starts++
    }

    override fun stop() {
        stops++
    }
}

private class ThrowingRegionDownloader : RegionDownloader {
    override suspend fun download(
        region: Region,
        onProgress: (fraction: Float?) -> Unit,
    ): EmptyResult<DataError.Network> = throw IllegalStateException("the download blew up")
}

private class FakeRegionUsageTracker : RegionUsageTracker {
    private val lastUsed = mutableMapOf<String, Long>()
    val clearCalls = mutableListOf<String>()

    override fun recordUsed(
        regionIds: Set<String>,
        atMillis: Long,
    ) {
        regionIds.forEach { lastUsed[it] = atMillis }
    }

    override fun lastUsedAt(regionId: String): Long? = lastUsed[regionId]

    override fun clear(regionId: String) {
        lastUsed.remove(regionId)
        clearCalls.add(regionId)
    }
}

class RegionDownloadControllerImplTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val downloader = FakeRegionDownloader().apply { progressSteps = listOf(0.5f, 1f) }
    private val catalog = FakeRegionCatalog()
    private val regionUsageTracker = FakeRegionUsageTracker()

    private fun createController(
        foregroundService: ForegroundServiceController = NoOpForegroundServiceController,
        regionDownloader: RegionDownloader = downloader,
    ): RegionDownloadControllerImpl =
        RegionDownloadControllerImpl(
            downloadRegion =
                DownloadRegionUseCase(
                    regionDownloader = regionDownloader,
                    regionCatalog = catalog,
                    recordRegionUsage = RecordRegionUsageUseCase(regionUsageTracker),
                    enforceStorageQuota =
                        EnforceStorageQuotaUseCase(
                            storageQuotaStore = FakeStorageQuotaStore(initialMaxBytes = null),
                            regionCatalog = catalog,
                            regionUsageTracker = regionUsageTracker,
                            draftWaypointsReader = DraftWaypointsHandoffImpl(),
                            lastComputedRouteReader = LastComputedRouteHandoffImpl(),
                        ),
                ),
            regionCatalog = catalog,
            regionUsageTracker = regionUsageTracker,
            foregroundServiceController = foregroundService,
            coroutineScope = CoroutineScope(dispatcher),
        )

    @Test
    fun `start begins a download and reports progress while in flight, then clears state on completion`() {
        val controller = createController()
        downloader.pauseNextDownload()

        controller.start(region)

        assertEquals(region, controller.state.value?.region)
        assertEquals(0.5f, controller.state.value?.progress)

        downloader.resume()

        assertNull(controller.state.value)
    }

    @Test
    fun `start while already downloading is a no-op`() {
        val controller = createController()
        downloader.pauseNextDownload()
        controller.start(region)
        val stateBefore = controller.state.value

        controller.start(otherRegion)

        assertEquals(stateBefore, controller.state.value)
        downloader.resume()
    }

    @Test
    fun `a failed download sends a Failed event and keeps already-downloaded tiles`() =
        runTest(dispatcher) {
            downloader.result = Result.Error(DataError.Network.NO_INTERNET)
            val controller = createController()
            val eventDeferred = async { controller.events.first() }

            controller.start(region)

            assertEquals(RegionDownloadEvent.Failed(region.id), eventDeferred.await())
            assertNull(controller.state.value)
            assertTrue(catalog.deleteRegionCalls.isEmpty())
        }

    @Test
    fun `a failed download with nobody listening still clears the state and stops the service`() {
        downloader.result = Result.Error(DataError.Network.NO_INTERNET)
        val service = CountingForegroundService()
        val controller = createController(service)

        controller.start(region)

        assertNull(controller.state.value)
        assertEquals(1, service.stops)
        downloader.result = Result.Success(Unit)
        controller.start(otherRegion)
        assertEquals(2, service.starts)
    }

    @Test
    fun `a failure that happened with nobody listening is delivered to the first listener`() =
        runTest(dispatcher) {
            downloader.result = Result.Error(DataError.Network.NO_INTERNET)
            val controller = createController()

            controller.start(region)

            assertEquals(RegionDownloadEvent.Failed(region.id), controller.events.first())
        }

    @Test
    fun `a download that throws clears the state, stops the service and reports a failure`() =
        runTest(dispatcher) {
            val service = CountingForegroundService()
            val controller = createController(service, ThrowingRegionDownloader())

            controller.start(region)

            assertNull(controller.state.value)
            assertEquals(1, service.stops)
            assertEquals(RegionDownloadEvent.Failed(region.id), controller.events.first())
        }

    @Test
    fun `cancel mid-download cancels the job, deletes the region's files, and clears usage tracking`() {
        val controller = createController()
        downloader.pauseNextDownload()
        controller.start(region)

        controller.cancel()

        assertEquals(listOf(region), catalog.deleteRegionCalls)
        assertEquals(listOf(region.id), regionUsageTracker.clearCalls)
        assertNull(controller.state.value)
    }

    @Test
    fun `cancel with nothing downloading is a no-op`() {
        val controller = createController()

        controller.cancel()

        assertTrue(catalog.deleteRegionCalls.isEmpty())
        assertNull(controller.state.value)
    }
}
