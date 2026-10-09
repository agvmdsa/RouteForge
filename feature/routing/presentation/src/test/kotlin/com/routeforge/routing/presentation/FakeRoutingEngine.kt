package com.routeforge.routing.presentation

import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.RoutingEngine
import com.routeforge.coredomain.model.Region

class FakeRoutingEngine : RoutingEngine {
    var snappableLatitudes: Set<Double> = emptySet()
    var computePathResult: Route? = null

    override fun snap(
        point: RoutePoint,
        availableRegions: List<Region>,
    ): RoutePoint =
        if (point.latitude in snappableLatitudes) {
            point.copy(snappedLatitude = point.latitude, snappedLongitude = point.longitude)
        } else {
            point
        }

    override fun computePath(points: List<RoutePoint>): Route? = computePathResult
}
