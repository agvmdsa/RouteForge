package com.routeforge.routing.presentation.regioncoveragemap

import com.routeforge.coredomain.model.MapStyle
import com.routeforge.coredomain.model.Region

internal data class RegionCoverageMapState(
    val regions: List<Region> = emptyList(),
    val tappedRegionLabel: String? = null,
    val mapStyle: MapStyle = MapStyle.DEFAULT,
)
