package com.routeforge.coredomain.holder

import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.StateFlow

/** Hand-off from the Favorites screen (picker mode) to route creation, which appends the value to
 *  the current draft as a new waypoint without any further confirmation — selecting an existing
 *  favorite is already an explicit, confirming action. */
class SelectedFavoriteWaypointHolder {
    private val holder = HandoffHolder<RoutePoint?>(initial = null)
    val selected: StateFlow<RoutePoint?> = holder.value

    fun set(point: RoutePoint) = holder.set(point)

    fun clear() = holder.set(null)
}
