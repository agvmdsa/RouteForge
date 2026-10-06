package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.FavoriteRoutesRepository
import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionUsage

/** Answers "would deleting this region break something the user is currently relying on" (spec
 *  006 FR-008) by reusing [ComputeRequiredRegionsUseCase] against three point sources: the
 *  current draft, the active/loaded route, and every saved favorite route. The warning this
 *  drives is informational only — it never blocks deletion (spec 006 FR-009). */
class ComputeRegionUsageUseCase(
    private val computeRequiredRegions: ComputeRequiredRegionsUseCase,
    private val draftWaypointsHolder: DraftWaypointsHolder,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
    private val favoriteRoutesRepository: FavoriteRoutesRepository,
) {
    operator fun invoke(region: Region): RegionUsage {
        fun touches(points: List<RoutePoint>) = computeRequiredRegions(points).regions.any { it.id == region.id }

        val neededByDraft = touches(draftWaypointsHolder.points.value)
        val neededByActiveRoute = lastComputedRouteHolder.route.value?.points?.let(::touches) ?: false
        val neededBySavedRouteNames =
            favoriteRoutesRepository.observeFavoriteRoutes().value
                .filter { touches(it.points) }
                .map { it.name }

        return RegionUsage(neededByDraft, neededByActiveRoute, neededBySavedRouteNames)
    }
}
