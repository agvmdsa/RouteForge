package com.routeforge.simulation.presentation.waypointedit

import com.routeforge.coredomain.RoutingEngine
import com.routeforge.coredomain.model.Region
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint

class FakeRoutingEngine : RoutingEngine {
    var computePathResult: Route? = null
    var computePathResults: List<Route?>? = null
    private var callCount = 0

    override fun snap(
        point: RoutePoint,
        availableRegions: List<Region>,
    ): RoutePoint = point.copy(snappedLatitude = point.latitude, snappedLongitude = point.longitude)

    override fun computePath(points: List<RoutePoint>): Route? {
        val sequence = computePathResults ?: return computePathResult
        val index = callCount.coerceAtMost(sequence.size - 1)
        callCount++
        return sequence[index]
    }
}
