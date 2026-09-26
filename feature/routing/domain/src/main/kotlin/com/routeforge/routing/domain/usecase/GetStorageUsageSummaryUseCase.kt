package com.routeforge.routing.domain.usecase

import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.model.RegionStatus

class GetStorageUsageSummaryUseCase(
    private val regionCatalog: RegionCatalog,
) {
    operator fun invoke(): Long =
        regionCatalog
            .listRegions()
            .filter { it.status == RegionStatus.DOWNLOADED || it.status == RegionStatus.PARTIALLY_DOWNLOADED }
            .sumOf { regionCatalog.actualSizeOnDiskBytes(it) }
}
