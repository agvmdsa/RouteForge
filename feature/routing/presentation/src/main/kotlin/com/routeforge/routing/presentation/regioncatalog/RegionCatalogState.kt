package com.routeforge.routing.presentation.regioncatalog

import com.routeforge.coredomain.model.Region
import com.routeforge.routing.domain.model.RegionUsage

data class RegionCatalogState(
    val regions: List<Region> = emptyList(),
    val neededRegionIds: Set<String> = emptySet(),
    val downloadingRegionId: String? = null,
    val downloadProgress: Float? = null,
    val pendingCancelDownload: Boolean = false,
    val pendingDeleteRegion: Region? = null,
    val deleteRegionUsage: RegionUsage? = null,
)
