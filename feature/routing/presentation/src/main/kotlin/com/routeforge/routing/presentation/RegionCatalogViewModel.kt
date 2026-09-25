package com.routeforge.routing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.DraftWaypointsHolder
import com.routeforge.coredomain.Result
import com.routeforge.routing.domain.model.RegionStatus
import com.routeforge.routing.domain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.routing.domain.usecase.DownloadRegionUseCase
import com.routeforge.routing.domain.usecase.ObserveRegionCatalogUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegionCatalogViewModel(
    private val observeRegionCatalog: ObserveRegionCatalogUseCase,
    private val downloadRegion: DownloadRegionUseCase,
    private val computeRequiredRegions: ComputeRequiredRegionsUseCase,
    private val draftWaypointsHolder: DraftWaypointsHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(RegionCatalogState())
    val state = _state.asStateFlow()

    private val _events = Channel<RegionCatalogEvent>()
    val events = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun onAction(action: RegionCatalogAction) {
        when (action) {
            RegionCatalogAction.OnRefresh -> refresh()
            is RegionCatalogAction.OnDownloadRegion -> startDownload(action.regionId)
        }
    }

    /** Regions the current route draft actually touches are surfaced first, so downloading stays
     *  localized to what's needed instead of the whole bundled catalog. A needed region may be a
     *  computed grid tile the catalog hasn't seen before (nothing downloaded for it yet, so it
     *  wouldn't otherwise appear in [observeRegionCatalog]) — merge it in so it's still downloadable.
     *  Catalog entries that are neither needed nor already downloaded (e.g. bundled manifest
     *  placeholders) are hidden — the list should only ever show what's relevant or what's already
     *  on the device, not every region the app happens to know about. */
    fun refresh() {
        val required = computeRequiredRegions(draftWaypointsHolder.points.value).regions
        val neededRegionIds = required.map { it.id }.toSet()
        val relevantCatalogRegions =
            observeRegionCatalog().filter { it.id in neededRegionIds || it.status != RegionStatus.NOT_DOWNLOADED }
        val regions =
            (required + relevantCatalogRegions)
                .distinctBy { it.id }
                .sortedByDescending { it.id in neededRegionIds }
        _state.update { it.copy(regions = regions, neededRegionIds = neededRegionIds) }
    }

    private fun startDownload(regionId: String) {
        _state.update { it.copy(downloadingRegionId = regionId, downloadProgress = 0f) }
        viewModelScope.launch {
            val result =
                downloadRegion(regionId) { fraction ->
                    _state.update { it.copy(downloadProgress = fraction) }
                }
            if (result is Result.Error) {
                _events.send(RegionCatalogEvent.DownloadFailed("Download failed. Check your connection and try again."))
            }
            _state.update { it.copy(downloadingRegionId = null, downloadProgress = 0f) }
            refresh()
        }
    }
}
