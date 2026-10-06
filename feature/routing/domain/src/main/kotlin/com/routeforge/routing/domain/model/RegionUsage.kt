package com.routeforge.routing.domain.model

/** Whether a region is still relied on by something, surfaced as a warning (never a hard block)
 *  before deleting it (spec 006 FR-008/FR-009). */
data class RegionUsage(
    val neededByDraft: Boolean,
    val neededByActiveRoute: Boolean,
    val neededBySavedRouteNames: List<String>,
) {
    val isInUse: Boolean
        get() = neededByDraft || neededByActiveRoute || neededBySavedRouteNames.isNotEmpty()
}
