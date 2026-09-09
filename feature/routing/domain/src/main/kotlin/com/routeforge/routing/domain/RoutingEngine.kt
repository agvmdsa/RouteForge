package com.routeforge.routing.domain

import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.Route
import com.routeforge.routing.domain.model.RoutePoint

interface RoutingEngine {
    fun snap(
        point: RoutePoint,
        availableRegions: List<Region>,
    ): RoutePoint

    fun computePath(points: List<RoutePoint>): Route?
}
