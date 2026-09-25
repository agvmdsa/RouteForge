package com.routeforge.routing.domain

import com.routeforge.routing.domain.model.Region

class FakeRegionCatalog(
    var regions: List<Region> = emptyList(),
) : RegionCatalog {
    override fun listRegions(): List<Region> = regions

    override fun regionContaining(
        latitude: Double,
        longitude: Double,
    ): Region? = regions.firstOrNull { it.contains(latitude, longitude) }

    override fun regionById(id: String): Region? = regions.firstOrNull { it.id == id }
}
