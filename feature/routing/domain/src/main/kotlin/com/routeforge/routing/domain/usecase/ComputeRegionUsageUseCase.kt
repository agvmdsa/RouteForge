package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.FavoriteRoutesRepository
import com.routeforge.coredomain.handoff.DraftWaypointsHandoffReader
import com.routeforge.coredomain.handoff.LastComputedRouteHandoffReader
import com.routeforge.coredomain.model.Region
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.routing.domain.model.RegionUsage

/** Answers "would deleting this region break something the user is currently relying on" (spec
 *  006 FR-008) by reusing [ComputeRequiredRegionsUseCase] against three point sources: the
 *  current draft, the active/loaded route, and every saved favorite route. The warning this
 *  drives is informational only — it never blocks deletion (spec 006 FR-009). */
class ComputeRegionUsageUseCase(
    private val computeRequiredRegions: ComputeRequiredRegionsUseCase,
    private val draftWaypointsReader: DraftWaypointsHandoffReader,
    private val lastComputedRouteReader: LastComputedRouteHandoffReader,
    private val favoriteRoutesRepository: FavoriteRoutesRepository,
) {
    operator fun invoke(region: Region): RegionUsage {
        fun touches(points: List<RoutePoint>) = computeRequiredRegions(points).regions.any { it.id == region.id }

        val neededByDraft = touches(draftWaypointsReader.points.value)
        val neededByActiveRoute =
            lastComputedRouteReader.route.value
                ?.points
                ?.let(::touches) ?: false
        val neededBySavedRouteNames =
            favoriteRoutesRepository
                .observeFavoriteRoutes()
                .value
                .filter { touches(it.points) }
                .map { it.name }

        return RegionUsage(neededByDraft, neededByActiveRoute, neededBySavedRouteNames)
    }
}
