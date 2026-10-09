package com.routeforge.simulation.presentation.waypointedit

import com.routeforge.coredomain.RegionCatalog
import com.routeforge.coredomain.model.Region

class FakeRegionCatalog(
    var regions: List<Region> = emptyList(),
) : RegionCatalog {
    override fun listRegions(): List<Region> = regions

    override fun regionContaining(
        latitude: Double,
        longitude: Double,
    ): Region? = regions.firstOrNull { it.contains(latitude, longitude) }

    override fun regionById(id: String): Region? = regions.firstOrNull { it.id == id }

    override fun actualSizeOnDiskBytes(region: Region): Long = 0L

    override fun deleteRegion(region: Region): Boolean {
        regions = regions.filterNot { it.id == region.id }
        return true
    }
}
