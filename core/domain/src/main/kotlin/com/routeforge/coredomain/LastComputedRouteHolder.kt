package com.routeforge.coredomain

import com.routeforge.coredomain.model.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LastComputedRouteHolder {
    private val _route = MutableStateFlow<Route?>(null)
    val route: StateFlow<Route?> = _route.asStateFlow()

    fun set(route: Route) {
        _route.value = route
    }

    fun clear() {
        _route.value = null
    }
}
