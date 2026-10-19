package com.routeforge.coredomain.holder

import com.routeforge.coredomain.model.IncomingRoute
import kotlinx.coroutines.flow.StateFlow

/** Hand-off from the app (which receives a `.gpx` file or a directions link) to route creation, which turns it into the draft,
 *  asking first when the draft already has waypoints. Route creation clears it once it has read it. */
class PendingIncomingRouteHolder {
    private val holder = HandoffHolder<IncomingRoute?>(initial = null)
    val route: StateFlow<IncomingRoute?> = holder.value

    fun set(route: IncomingRoute) = holder.set(route)

    fun clear() = holder.set(null)
}
