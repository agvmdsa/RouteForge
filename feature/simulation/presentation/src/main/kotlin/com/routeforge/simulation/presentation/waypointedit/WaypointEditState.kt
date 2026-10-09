package com.routeforge.simulation.presentation.waypointedit

import com.routeforge.coredomain.model.ModeAvailability
import com.routeforge.coredomain.model.RequiredRegionsSummary
import com.routeforge.coredomain.model.RoutePoint

/** State for Simulate's shared waypoint-edit sheet (spec 008). A second, sibling ViewModel's
 *  state to [com.routeforge.simulation.presentation.SimulationState] — research.md Decision 9 —
 *  so [com.routeforge.simulation.presentation.SimulationViewModel] doesn't grow this feature's own
 *  half-dozen fields directly. */
data class WaypointEditState(
    val isOpen: Boolean = false,
    val isSessionActive: Boolean = false,
    val points: List<RoutePoint> = emptyList(),
    /** True once the list has been reordered locally but not yet committed/recomputed — reordering
     *  only updates display order (fast, no lag mid-drag); [WaypointEditAction.OnApplyChanges]
     *  triggers the actual recompute for whatever order is current at that point. */
    val hasUnappliedReorder: Boolean = false,
    val firstEditableIndex: Int = 0,
    val inactiveModeAvailability: ModeAvailability = ModeAvailability.AVAILABLE,
    val missingRegionsWarning: RequiredRegionsSummary? = null,
    val pendingEditPoints: List<RoutePoint>? = null,
    val errorMessage: String? = null,
    val pendingAddLatitude: Double? = null,
    val pendingAddLongitude: Double? = null,
    val isSaveAsFavoriteChecked: Boolean = false,
    val favoriteNameInput: String = "",
)
