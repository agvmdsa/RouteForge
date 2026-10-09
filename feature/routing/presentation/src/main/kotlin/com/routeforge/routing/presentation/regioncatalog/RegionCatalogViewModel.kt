package com.routeforge.routing.presentation.regioncatalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.routing.domain.RegionDownloadController
import com.routeforge.routing.domain.RegionDownloadEvent
import com.routeforge.coredomain.model.RegionStatus
import com.routeforge.routing.domain.usecase.ComputeRegionUsageUseCase
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.routing.domain.usecase.DeleteRegionUseCase
import com.routeforge.routing.domain.usecase.ObserveRegionCatalogUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegionCatalogViewModel(
    private val observeRegionCatalog: ObserveRegionCatalogUseCase,
    private val regionDownloadController: RegionDownloadController,
    private val computeRequiredRegions: ComputeRequiredRegionsUseCase,
    private val draftWaypointsHolder: DraftWaypointsHolder,
    private val deleteRegion: DeleteRegionUseCase,
    private val computeRegionUsage: ComputeRegionUsageUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(RegionCatalogState())
    val state = _state.asStateFlow()

    private val _events = Channel<RegionCatalogEvent>()
    val events = _events.receiveAsFlow()

    /** [regionDownloadController] intentionally outlives this ViewModel — it owns its own
     *  CoroutineScope (started in feature:routing:data's DI module), so a download keeps running
     *  if this screen is left or the app is minimized (spec 006 FR-003/FR-004). This ViewModel
     *  only ever mirrors its state; it never owns the download itself. */
    init {
        refresh()

        regionDownloadController.state
            .onEach { downloadState ->
                _state.update {
                    it.copy(downloadingRegionId = downloadState?.region?.id, downloadProgress = downloadState?.progress)
                }
                if (downloadState == null) refresh()
            }.launchIn(viewModelScope)

        regionDownloadController.events
            .onEach { event ->
                when (event) {
                    is RegionDownloadEvent.Failed ->
                        _events.send(RegionCatalogEvent.DownloadFailed("Download failed. Check your connection and try again."))
                }
            }.launchIn(viewModelScope)
    }

    fun onAction(action: RegionCatalogAction) {
        when (action) {
            RegionCatalogAction.OnRefresh -> refresh()
            is RegionCatalogAction.OnDownloadRegion -> startDownload(action.regionId)
            RegionCatalogAction.OnCancelDownloadClick -> _state.update { it.copy(pendingCancelDownload = true) }
            RegionCatalogAction.OnConfirmCancelDownload -> {
                regionDownloadController.cancel()
                _state.update { it.copy(pendingCancelDownload = false) }
            }
            RegionCatalogAction.OnDismissCancelDownload -> _state.update { it.copy(pendingCancelDownload = false) }
            is RegionCatalogAction.OnDeleteRegionClick -> onDeleteRegionClick(action.regionId)
            RegionCatalogAction.OnConfirmDeleteRegion -> confirmDeleteRegion()
            RegionCatalogAction.OnDismissDeleteRegion ->
                _state.update { it.copy(pendingDeleteRegion = null, deleteRegionUsage = null) }
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
        val region = _state.value.regions.firstOrNull { it.id == regionId } ?: return
        regionDownloadController.start(region)
    }

    /** No-op for the currently-downloading region — only cancelling applies there (spec 006
     *  FR-011); delete is only ever offered for DOWNLOADED/PARTIALLY_DOWNLOADED cards in the UI,
     *  but this guard keeps the ViewModel correct even if that ever changes. */
    private fun onDeleteRegionClick(regionId: String) {
        if (regionId == _state.value.downloadingRegionId) return
        val region = _state.value.regions.firstOrNull { it.id == regionId } ?: return
        _state.update { it.copy(pendingDeleteRegion = region, deleteRegionUsage = computeRegionUsage(region)) }
    }

    /** Deletes regardless of [RegionCatalogState.deleteRegionUsage] — the usage warning is
     *  informational only and never blocks deletion (spec 006 FR-009). */
    private fun confirmDeleteRegion() {
        val region = _state.value.pendingDeleteRegion ?: return
        deleteRegion(region)
        _state.update { it.copy(pendingDeleteRegion = null, deleteRegionUsage = null) }
        refresh()
    }
}
