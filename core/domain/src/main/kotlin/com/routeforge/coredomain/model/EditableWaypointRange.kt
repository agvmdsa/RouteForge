package com.routeforge.coredomain.model

/** The subset of a route's waypoints that can currently be modified (spec 008's Key Entities
 *  "Editable Range"). Waypoint indices `< firstEditableIndex` are already passed and read-only;
 *  indices `>= firstEditableIndex` are editable. `firstEditableIndex == 0` means every waypoint
 *  is editable (the loaded-but-not-running case, or when boundary info isn't available yet). Not
 *  persisted — a computed view over a [Route] and a session's current progress. */
data class EditableWaypointRange(
    val firstEditableIndex: Int,
)
