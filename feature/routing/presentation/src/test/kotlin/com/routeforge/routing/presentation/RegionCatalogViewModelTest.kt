package com.routeforge.routing.presentation

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.DraftWaypointsHolder
import com.routeforge.coredomain.EmptyResult
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.RegionDownloader
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import com.routeforge.routing.domain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.routing.domain.usecase.DownloadRegionUseCase
import com.routeforge.routing.domain.usecase.ObserveRegionCatalogUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val downloadableRegion =
    Region(
        id = "region-1",
        displayName = "Region 1",
        minLatitude = 0.0,
        minLongitude = 0.0,
        maxLatitude = 1.0,
        maxLongitude = 1.0,
        tileIds = listOf("tile.rd5"),
        approximateSizeBytes = 10L,
        status = RegionStatus.NOT_DOWNLOADED,
    )

private val otherRegion =
    Region(
        id = "region-2",
        displayName = "Region 2",
        minLatitude = 5.0,
        minLongitude = 5.0,
        maxLatitude = 6.0,
        maxLongitude = 6.0,
        tileIds = listOf("other.rd5"),
        approximateSizeBytes = 20L,
        status = RegionStatus.DOWNLOADED,
    )

private class FakeRegionDownloader(
    var result: EmptyResult<DataError.Network> = Result.Success(Unit),
) : RegionDownloader {
    override suspend fun download(
        region: Region,
        onProgress: (fraction: Float) -> Unit,
    ): EmptyResult<DataError.Network> {
        onProgress(0.5f)
        onProgress(1f)
        return result
    }
}

class RegionCatalogViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val draftWaypointsHolder = DraftWaypointsHolder()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        catalog: FakeRegionCatalog,
        downloader: RegionDownloader = FakeRegionDownloader(),
    ): RegionCatalogViewModel =
        RegionCatalogViewModel(
            observeRegionCatalog = ObserveRegionCatalogUseCase(catalog),
            downloadRegion = DownloadRegionUseCase(downloader, catalog),
            computeRequiredRegions = ComputeRequiredRegionsUseCase(catalog),
            draftWaypointsHolder = draftWaypointsHolder,
        )

    @Test
    fun `initial state reflects the current region catalog`() {
        val catalog = FakeRegionCatalog(regions = listOf(downloadableRegion))
        val viewModel = createViewModel(catalog)

        assertEquals(listOf(downloadableRegion), viewModel.state.value.regions)
    }

    @Test
    fun `regions touched by the current draft are surfaced first and flagged as needed`() {
        val catalog = FakeRegionCatalog(regions = listOf(downloadableRegion, otherRegion))
        draftWaypointsHolder.set(listOf(RoutePoint(latitude = 5.5, longitude = 5.5)))

        val viewModel = createViewModel(catalog)

        assertEquals(listOf(otherRegion, downloadableRegion), viewModel.state.value.regions)
        assertEquals(setOf(otherRegion.id), viewModel.state.value.neededRegionIds)
    }

    @Test
    fun `downloading a region reports progress then clears the downloading id`() {
        val catalog = FakeRegionCatalog(regions = listOf(downloadableRegion))
        val downloader = FakeRegionDownloader()
        val viewModel = createViewModel(catalog, downloader)

        viewModel.onAction(RegionCatalogAction.OnDownloadRegion(downloadableRegion.id))

        assertNull(viewModel.state.value.downloadingRegionId)
        assertEquals(0f, viewModel.state.value.downloadProgress)
    }

    @Test
    fun `a failed download emits a download failed event`() =
        runTest(dispatcher) {
            val catalog = FakeRegionCatalog(regions = listOf(downloadableRegion))
            val downloader = FakeRegionDownloader(result = Result.Error(DataError.Network.NO_INTERNET))
            val viewModel = createViewModel(catalog, downloader)

            val eventDeferred = async { viewModel.events.first() }
            viewModel.onAction(RegionCatalogAction.OnDownloadRegion(downloadableRegion.id))

            assertEquals(
                RegionCatalogEvent.DownloadFailed("Download failed. Check your connection and try again."),
                eventDeferred.await(),
            )
        }
}
