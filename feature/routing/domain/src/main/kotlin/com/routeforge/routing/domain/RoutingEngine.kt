package com.routeforge.routing.domain

import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.model.Region

interface RoutingEngine {
    fun snap(
        point: RoutePoint,
        availableRegions: List<Region>,
    ): RoutePoint

    fun computePath(points: List<RoutePoint>): Route?
}
