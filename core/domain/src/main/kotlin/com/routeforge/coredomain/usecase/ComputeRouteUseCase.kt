package com.routeforge.coredomain.usecase

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.RegionCatalog
import com.routeforge.coredomain.RoutingEngine
import com.routeforge.coredomain.RoutingFailure

class ComputeRouteUseCase(
    private val routingEngine: RoutingEngine,
    private val regionCatalog: RegionCatalog,
    private val recordRegionUsage: RecordRegionUsageUseCase,
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
            val touchedRegionIds = points.mapNotNull { regionCatalog.regionContaining(it.latitude, it.longitude)?.id }.toSet()
            recordRegionUsage(touchedRegionIds)
            Result.Success(route)
        }
    }
}
