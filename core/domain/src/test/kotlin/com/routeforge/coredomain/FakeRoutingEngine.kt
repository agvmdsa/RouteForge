package com.routeforge.coredomain

import com.routeforge.coredomain.RoutingEngine

import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.model.Region

class FakeRoutingEngine : RoutingEngine {
    var snapResult: (RoutePoint) -> RoutePoint = {
        it.copy(snappedLatitude = it.latitude, snappedLongitude = it.longitude)
    }
    var computePathResult: Route? = null

    /** When non-null, overrides [computePathResult] with a per-call sequence (consumed in order,
     *  the last entry sticking once exhausted) — lets a test simulate a transient failure that
     *  resolves on a later attempt, e.g. spec 008's FR-011 immediate-retry behavior. */
    var computePathResults: List<Route?>? = null
    private var computePathCallCount = 0

    override fun snap(
        point: RoutePoint,
        availableRegions: List<Region>,
    ): RoutePoint = snapResult(point)

    override fun computePath(points: List<RoutePoint>): Route? {
        val sequence = computePathResults ?: return computePathResult
        val index = computePathCallCount.coerceAtMost(sequence.size - 1)
        computePathCallCount++
        return sequence[index]
    }
}
