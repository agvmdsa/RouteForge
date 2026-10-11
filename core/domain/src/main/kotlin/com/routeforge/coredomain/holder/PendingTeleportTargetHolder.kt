package com.routeforge.coredomain.holder

import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.StateFlow

/** Hand-off from the Favorites screen's "mock here" confirmation to the Simulation screen, which
 *  teleports immediately on a new value without asking again — the confirmation already happened
 *  where the value was set. */
class PendingTeleportTargetHolder {
    private val holder = HandoffHolder<RoutePoint?>(initial = null)
    val target: StateFlow<RoutePoint?> = holder.value

    fun set(point: RoutePoint) = holder.set(point)

    fun clear() = holder.set(null)
}
