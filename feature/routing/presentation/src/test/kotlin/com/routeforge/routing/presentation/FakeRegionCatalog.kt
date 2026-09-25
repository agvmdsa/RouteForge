package com.routeforge.routing.presentation

import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.model.Region

class FakeRegionCatalog(
    var regions: List<Region> = emptyList(),
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
}
