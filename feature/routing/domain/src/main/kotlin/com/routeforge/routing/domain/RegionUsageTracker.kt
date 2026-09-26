package com.routeforge.routing.domain

/** Tracks the most recent time a downloaded region was actually used to compute or play a route —
 *  used to rank removal order when the storage budget is exceeded (FR-005). */
interface RegionUsageTracker {
    fun recordUsed(
        regionIds: Set<String>,
        atMillis: Long = System.currentTimeMillis(),
    )

    /** Null if this region has never been recorded as used. */
    fun lastUsedAt(regionId: String): Long?

    fun clear(regionId: String)
}
