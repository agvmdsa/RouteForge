package com.routeforge.routing.presentation.regioncoveragemap

import androidx.lifecycle.ViewModel
import com.routeforge.routing.domain.usecase.ObserveRegionCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Read-only snapshot of the region catalog at the time this screen opens — no live refresh
 *  needed, since nothing on this screen can change which regions are downloaded. */
class RegionCoverageMapViewModel(
    observeRegionCatalog: ObserveRegionCatalogUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(RegionCoverageMapState(regions = observeRegionCatalog()))
    val state = _state.asStateFlow()

    fun onAction(action: RegionCoverageMapAction) {
        when (action) {
            is RegionCoverageMapAction.OnMapTapped -> onMapTapped(action.latitude, action.longitude)
            RegionCoverageMapAction.OnDismissTappedRegion -> _state.update { it.copy(tappedRegionLabel = null) }
        }
    }

    private fun onMapTapped(
        latitude: Double,
        longitude: Double,
    ) {
        val region = _state.value.regions.firstOrNull { it.contains(latitude, longitude) }
        _state.update { it.copy(tappedRegionLabel = region?.tileIds?.firstOrNull() ?: region?.displayName) }
    }
}
