package com.routeforge.routing.presentation

import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.model.Region

class FakeRegionCatalog(
    var regions: List<Region> = emptyList(),
    var sizesOnDiskBytes: Map<String, Long> = emptyMap(),
) : RegionCatalog {
    override fun listRegions(): List<Region> = regions

    override fun regionContaining(
        latitude: Double,
        longitude: Double,
    ): Region? =
        regions.firstOrNull {
            latitude in it.minLatitude..it.maxLatitude && longitude in it.minLongitude..it.maxLongitude
        }

    override fun regionById(id: String): Region? = regions.firstOrNull { it.id == id }

    override fun actualSizeOnDiskBytes(region: Region): Long = sizesOnDiskBytes[region.id] ?: 0L

    override fun deleteRegion(region: Region): Boolean {
        regions = regions.filterNot { it.id == region.id }
        sizesOnDiskBytes = sizesOnDiskBytes - region.id
        return true
    }
}
