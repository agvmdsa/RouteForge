package com.routeforge.routing.presentation.regioncoveragemap

import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import com.routeforge.routing.domain.usecase.ObserveRegionCatalogUseCase
import com.routeforge.routing.presentation.FakeRegionCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private val downloadedRegion =
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

private val notDownloadedRegion =
    Region(
        id = "region-2",
        displayName = "Region 2",
        minLatitude = 5.0,
        minLongitude = 5.0,
        maxLatitude = 6.0,
        maxLongitude = 6.0,
        tileIds = listOf("region-2.rd5"),
        approximateSizeBytes = 20L,
        status = RegionStatus.NOT_DOWNLOADED,
    )

class RegionCoverageMapViewModelTest {
    private fun viewModel(regions: List<Region>): RegionCoverageMapViewModel {
        val regionCatalog = FakeRegionCatalog(regions = regions)
        return RegionCoverageMapViewModel(observeRegionCatalog = ObserveRegionCatalogUseCase(regionCatalog))
    }

    @Test
    fun `initial state lists every known region regardless of download status`() {
        val viewModel = viewModel(listOf(downloadedRegion, notDownloadedRegion))

        assertEquals(listOf(downloadedRegion, notDownloadedRegion), viewModel.state.value.regions)
        assertNull(viewModel.state.value.tappedRegionLabel)
    }

    @Test
    fun `tapping a point inside a region reports that region's tile file`() {
        val viewModel = viewModel(listOf(downloadedRegion, notDownloadedRegion))

        viewModel.onAction(RegionCoverageMapAction.OnMapTapped(latitude = 0.5, longitude = 0.5))

        assertEquals("region-1.rd5", viewModel.state.value.tappedRegionLabel)
    }

    @Test
    fun `tapping a point outside every known region clears any previous label`() {
        val viewModel = viewModel(listOf(downloadedRegion))
        viewModel.onAction(RegionCoverageMapAction.OnMapTapped(latitude = 0.5, longitude = 0.5))

        viewModel.onAction(RegionCoverageMapAction.OnMapTapped(latitude = 50.0, longitude = 50.0))

        assertNull(viewModel.state.value.tappedRegionLabel)
    }

    @Test
    fun `dismissing the tapped region clears the label`() {
        val viewModel = viewModel(listOf(downloadedRegion))
        viewModel.onAction(RegionCoverageMapAction.OnMapTapped(latitude = 0.5, longitude = 0.5))

        viewModel.onAction(RegionCoverageMapAction.OnDismissTappedRegion)

        assertNull(viewModel.state.value.tappedRegionLabel)
    }
}
