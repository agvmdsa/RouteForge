package com.routeforge.routing.presentation.routerequest.waypointedit

import com.routeforge.coredomain.model.ModeAvailability
import com.routeforge.coredomain.model.RequiredRegionsSummary
import com.routeforge.coredomain.model.RoutePoint

/** State for Plan Route's shared waypoint-edit sheet (spec 008) — a second, sibling ViewModel's
 *  state to [com.routeforge.routing.presentation.routerequest.RouteRequestState], research.md
 *  Decision 9. Plan Route's own existing map-tap-add/marker-drag-move paths are left unchanged
 *  (they predate this feature and already work); this state only covers the new reorder-list's
 *  own reorder/delete actions. */
data class WaypointEditState(
    val isOpen: Boolean = false,
    val points: List<RoutePoint> = emptyList(),
    /** True once the list has been reordered locally but not yet committed/recomputed — reordering
     *  only updates display order (fast, no lag mid-drag); [WaypointEditAction.OnApplyChanges]
     *  triggers the actual recompute for whatever order is current at that point. */
    val hasUnappliedReorder: Boolean = false,
    val inactiveModeAvailability: ModeAvailability = ModeAvailability.AVAILABLE,
    val missingRegionsWarning: RequiredRegionsSummary? = null,
    val pendingEditPoints: List<RoutePoint>? = null,
    val errorMessage: String? = null,
)
