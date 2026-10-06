package com.routeforge.coredomain.holder

import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Hand-off from the Favorites screen (picker mode) to route creation, which appends the value to
 *  the current draft as a new waypoint without any further confirmation — selecting an existing
 *  favorite is already an explicit, confirming action. */
class SelectedFavoriteWaypointHolder {
    private val _selected = MutableStateFlow<RoutePoint?>(null)
    val selected: StateFlow<RoutePoint?> = _selected.asStateFlow()

    fun set(point: RoutePoint) {
        _selected.value = point
    }

    fun clear() {
        _selected.value = null
    }
}
