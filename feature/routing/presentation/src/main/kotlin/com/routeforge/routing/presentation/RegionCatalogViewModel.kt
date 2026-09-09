package com.routeforge.routing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.Result
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

    fun refresh() {
        _state.update { it.copy(regions = observeRegionCatalog()) }
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
