package com.routeforge.coredomain

import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.model.Region

interface RoutingEngine {
    fun snap(
        point: RoutePoint,
        availableRegions: List<Region>,
    ): RoutePoint

    fun computePath(points: List<RoutePoint>): Route?
}
