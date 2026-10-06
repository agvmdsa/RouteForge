package com.routeforge.coredomain.holder

import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Hand-off from the Favorites screen's "mock here" confirmation to the Simulation screen, which
 *  teleports immediately on a new value without asking again — the confirmation already happened
 *  where the value was set. */
class PendingTeleportTargetHolder {
    private val _target = MutableStateFlow<RoutePoint?>(null)
    val target: StateFlow<RoutePoint?> = _target.asStateFlow()

    fun set(point: RoutePoint) {
        _target.value = point
    }

    fun clear() {
        _target.value = null
    }
}
