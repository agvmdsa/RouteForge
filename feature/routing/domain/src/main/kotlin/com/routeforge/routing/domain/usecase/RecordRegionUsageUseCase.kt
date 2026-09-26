package com.routeforge.routing.domain.usecase

import com.routeforge.routing.domain.RegionUsageTracker

class RecordRegionUsageUseCase(
    private val regionUsageTracker: RegionUsageTracker,
) {
    operator fun invoke(regionIds: Set<String>) {
        if (regionIds.isEmpty()) return
        regionUsageTracker.recordUsed(regionIds)
    }
}
