package com.routeforge.coredomain.usecase

import com.routeforge.coredomain.RegionUsageTracker

class RecordRegionUsageUseCase(
    private val regionUsageTracker: RegionUsageTracker,
) {
    operator fun invoke(regionIds: Set<String>) {
        if (regionIds.isEmpty()) return
        regionUsageTracker.recordUsed(regionIds)
    }
}
