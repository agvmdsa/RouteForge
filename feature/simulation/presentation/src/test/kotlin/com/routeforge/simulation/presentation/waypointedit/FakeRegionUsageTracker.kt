package com.routeforge.simulation.presentation.waypointedit

import com.routeforge.coredomain.RegionUsageTracker

class FakeRegionUsageTracker : RegionUsageTracker {
    private val usage = mutableMapOf<String, Long>()

    override fun recordUsed(
        regionIds: Set<String>,
        atMillis: Long,
    ) {
        for (regionId in regionIds) usage[regionId] = atMillis
    }

    override fun lastUsedAt(regionId: String): Long? = usage[regionId]

    override fun clear(regionId: String) {
        usage.remove(regionId)
    }
}
