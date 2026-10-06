package com.routeforge.routing.domain.usecase

import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.RegionUsageTracker
import com.routeforge.routing.domain.model.Region

/** Deletes a fully-or-partially downloaded region to free storage, on explicit user request
 *  (spec 006 FR-007/FR-010) — distinct from [EnforceStorageQuotaUseCase]'s automatic eviction, but
 *  reuses the exact same two primitives it already calls per region. */
class DeleteRegionUseCase(
    private val regionCatalog: RegionCatalog,
    private val regionUsageTracker: RegionUsageTracker,
) {
    operator fun invoke(region: Region): Boolean {
        val deleted = regionCatalog.deleteRegion(region)
        if (deleted) regionUsageTracker.clear(region.id)
        return deleted
    }
}
