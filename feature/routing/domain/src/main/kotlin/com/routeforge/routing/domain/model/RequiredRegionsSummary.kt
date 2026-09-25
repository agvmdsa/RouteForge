package com.routeforge.routing.domain.model

/** What map data a draft's waypoints actually touch, used to gate Guided mode and to drive the region catalog's downloads.
 *  Waypoints with no matching region at all ([uncoveredWaypointCount]) don't block [isFullyDownloaded] — that's a
 *  "no map data exists for this area" gap, not a "download it" gap, and Guided already silently falls back to
 *  Free-roam for those per FR-003. */
data class RequiredRegionsSummary(
    val regions: List<Region>,
    val uncoveredWaypointCount: Int,
) {
    val isFullyDownloaded: Boolean
        get() = regions.all { it.status == RegionStatus.DOWNLOADED }

    val totalMissingBytes: Long
        get() = regions.sumOf { it.estimatedMissingBytes }
}
