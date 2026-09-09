package com.routeforge.routing.domain.usecase

import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.model.Region

class ObserveRegionCatalogUseCase(
    private val regionCatalog: RegionCatalog,
) {
    operator fun invoke(): List<Region> = regionCatalog.listRegions()
}
