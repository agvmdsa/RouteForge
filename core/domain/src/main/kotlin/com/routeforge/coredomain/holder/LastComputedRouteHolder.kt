package com.routeforge.coredomain.holder

import com.routeforge.coredomain.model.Route
import kotlinx.coroutines.flow.StateFlow

class LastComputedRouteHolder {
    private val holder = HandoffHolder<Route?>(initial = null)
    val route: StateFlow<Route?> = holder.value

    fun set(route: Route) = holder.set(route)

    fun clear() = holder.set(null)
}
