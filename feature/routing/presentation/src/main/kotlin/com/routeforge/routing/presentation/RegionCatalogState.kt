package com.routeforge.routing.presentation

import com.routeforge.routing.domain.model.Region

data class RegionCatalogState(
    val regions: List<Region> = emptyList(),
    val downloadingRegionId: String? = null,
    val downloadProgress: Float = 0f,
)
