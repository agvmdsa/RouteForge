package com.routeforge.coredomain.holder

import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.StateFlow

/** The route-creation screen's current waypoints, kept here so other screens (e.g. the region
 *  catalog) can tell what map data is actually needed without a direct feature-to-feature dependency. */
class DraftWaypointsHolder {
    private val holder = HandoffHolder<List<RoutePoint>>(initial = emptyList())
    val points: StateFlow<List<RoutePoint>> = holder.value

    fun set(points: List<RoutePoint>) = holder.set(points)
}
