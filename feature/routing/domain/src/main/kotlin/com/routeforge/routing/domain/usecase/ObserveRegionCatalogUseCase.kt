package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.RegionCatalog
import com.routeforge.coredomain.model.Region

class ObserveRegionCatalogUseCase(
    private val regionCatalog: RegionCatalog,
) {
    operator fun invoke(): List<Region> = regionCatalog.listRegions()
}
