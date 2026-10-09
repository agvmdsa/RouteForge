package com.routeforge.routing.data

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.EmptyResult
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.RegionCatalog
import com.routeforge.routing.domain.RegionDownloadEvent
import com.routeforge.routing.domain.RegionDownloader
import com.routeforge.coredomain.RegionUsageTracker
import com.routeforge.routing.domain.StorageQuotaStore
import com.routeforge.coredomain.model.Region
import com.routeforge.coredomain.model.RegionStatus
import com.routeforge.routing.domain.usecase.DownloadRegionUseCase
import com.routeforge.routing.domain.usecase.EnforceStorageQuotaUseCase
import com.routeforge.coredomain.usecase.RecordRegionUsageUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

private class FakeRegionDownloader : RegionDownloader {
    var result: EmptyResult<DataError.Network> = Result.Success(Unit)
    private var gate: CompletableDeferred<Unit>? = null

    fun pauseNextDownload() {
        gate = CompletableDeferred()
    }

    fun resume() {
        gate?.complete(Unit)
    }

    override suspend fun download(
        region: Region,
        onProgress: (fraction: Float?) -> Unit,
    ): EmptyResult<DataError.Network> {
        onProgress(0.5f)
        gate?.await()
        onProgress(1f)
        return result
    }
}

private class FakeRegionCatalog : RegionCatalog {
    val deleteRegionCalls = mutableListOf<Region>()

    override fun listRegions(): List<Region> = emptyList()

    override fun regionContaining(
        latitude: Double,
        longitude: Double,
    ): Region? = null

    override fun regionById(id: String): Region? = listOf(region, otherRegion).firstOrNull { it.id == id }

    override fun actualSizeOnDiskBytes(region: Region): Long = 0L

    override fun deleteRegion(region: Region): Boolean {
        deleteRegionCalls.add(region)
        return true
    }
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

private class FakeStorageQuotaStore : StorageQuotaStore {
    private val _maxBytes = MutableStateFlow<Long?>(null)

    override fun observeMaxBytes(): StateFlow<Long?> = _maxBytes

    override fun setMaxBytes(maxBytes: Long?) {
        _maxBytes.value = maxBytes
    }
}

class RegionDownloadControllerImplTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val downloader = FakeRegionDownloader()
    private val catalog = FakeRegionCatalog()
    private val regionUsageTracker = FakeRegionUsageTracker()

    private fun createController(): RegionDownloadControllerImpl =
        RegionDownloadControllerImpl(
            downloadRegion =
                DownloadRegionUseCase(
                    regionDownloader = downloader,
                    regionCatalog = catalog,
                    recordRegionUsage = RecordRegionUsageUseCase(regionUsageTracker),
                    enforceStorageQuota =
                        EnforceStorageQuotaUseCase(
                            storageQuotaStore = FakeStorageQuotaStore(),
                            regionCatalog = catalog,
                            regionUsageTracker = regionUsageTracker,
                            draftWaypointsHolder = DraftWaypointsHolder(),
                            lastComputedRouteHolder = LastComputedRouteHolder(),
                        ),
                ),
            regionCatalog = catalog,
            regionUsageTracker = regionUsageTracker,
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
