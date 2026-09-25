package com.routeforge.coredomain

import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The route-creation screen's current waypoints, kept here so other screens (e.g. the region
 *  catalog) can tell what map data is actually needed without a direct feature-to-feature dependency. */
class DraftWaypointsHolder {
    private val _points = MutableStateFlow<List<RoutePoint>>(emptyList())
    val points: StateFlow<List<RoutePoint>> = _points.asStateFlow()

    fun set(points: List<RoutePoint>) {
        _points.value = points
    }
}
