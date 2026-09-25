package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RequiredRegionsSummary

/** Maps a draft's waypoints to the distinct regions covering them, so callers can tell upfront
 *  whether Guided mode's map data is fully in place before computing a route. */
class ComputeRequiredRegionsUseCase(
    private val regionCatalog: RegionCatalog,
) {
    operator fun invoke(points: List<RoutePoint>): RequiredRegionsSummary {
        val touchedRegions = LinkedHashSet<Region>()
        var uncoveredWaypointCount = 0
        for (point in points) {
            val region = regionCatalog.regionContaining(point.latitude, point.longitude)
            if (region == null) uncoveredWaypointCount++ else touchedRegions.add(region)
        }
        return RequiredRegionsSummary(touchedRegions.toList(), uncoveredWaypointCount)
    }
}
