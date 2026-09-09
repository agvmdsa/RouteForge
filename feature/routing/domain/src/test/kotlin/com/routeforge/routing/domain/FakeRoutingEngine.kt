package com.routeforge.routing.domain

import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.Route
import com.routeforge.routing.domain.model.RoutePoint

class FakeRoutingEngine : RoutingEngine {
    var snapResult: (RoutePoint) -> RoutePoint = {
        it.copy(snappedLatitude = it.latitude, snappedLongitude = it.longitude)
    }
    var computePathResult: Route? = null

    override fun snap(
        point: RoutePoint,
        availableRegions: List<Region>,
    ): RoutePoint = snapResult(point)

    override fun computePath(points: List<RoutePoint>): Route? = computePathResult
}
