package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.RoutingEngine
import com.routeforge.routing.domain.RoutingFailure

class ComputeRouteUseCase(
    private val routingEngine: RoutingEngine,
    private val regionCatalog: RegionCatalog,
) {
    operator fun invoke(points: List<RoutePoint>): Result<Route, RoutingFailure> {
        val availableRegions = regionCatalog.listRegions()
        val snappedPoints = mutableListOf<RoutePoint>()

        for (point in points) {
            val snapped = routingEngine.snap(point, availableRegions)
            if (snapped.snappedLatitude == null || snapped.snappedLongitude == null) {
                val isInsideKnownRegion = regionCatalog.regionContaining(point.latitude, point.longitude) != null
                val failure =
                    if (isInsideKnownRegion) {
                        RoutingFailure.PointNotRoutable(point)
                    } else {
                        RoutingFailure.PointOutsideAvailableCoverage(point)
                    }
                return Result.Error(failure)
            }
            snappedPoints.add(snapped)
        }

        val route = routingEngine.computePath(snappedPoints)
        return if (route == null) {
            Result.Error(RoutingFailure.NoPathBetweenPoints)
        } else {
            Result.Success(route)
        }
    }
}
