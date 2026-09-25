package com.routeforge.routing.domain

import com.routeforge.routing.domain.model.Region

interface RegionCatalog {
    fun listRegions(): List<Region>

    fun regionContaining(
        latitude: Double,
        longitude: Double,
    ): Region?

    /** Resolves a region by id, synthesizing one from the BRouter tile grid if [id] isn't already
     *  in [listRegions] — lets downloads target a computed tile the user hasn't fetched before. */
    fun regionById(id: String): Region?
}
