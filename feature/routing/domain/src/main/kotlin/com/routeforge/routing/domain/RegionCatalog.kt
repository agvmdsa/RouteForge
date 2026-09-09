package com.routeforge.routing.domain

import com.routeforge.routing.domain.model.Region

interface RegionCatalog {
    fun listRegions(): List<Region>

    fun regionContaining(
        latitude: Double,
        longitude: Double,
    ): Region?
}
