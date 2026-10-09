package com.routeforge.coredomain.usecase

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.ModeAvailability
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint

private const val INACTIVE_GUIDED_IMMEDIATE_RETRY_ATTEMPTS = 2

/** Orchestrates spec 008's FR-009/FR-010/FR-011 for a single edit: a region check, then
 *  computing both playback modes for the edited waypoint list, with the currently active mode
 *  gatekeeping the whole edit and the inactive mode recomputing best-effort. Free-roam always
 *  succeeds by construction ([FreeRoamRouteBuilder]), so the only mode that can ever need the
 *  FR-011 immediate-retry treatment is Guided when it's the inactive one. */
class RecomputeRouteForBothModesUseCase(
    private val computeRequiredRegions: ComputeRequiredRegionsUseCase,
    private val prepareRouteOptions: PrepareRouteOptionsUseCase,
    private val computeRoute: ComputeRouteUseCase,
) {
    operator fun invoke(
        points: List<RoutePoint>,
        activeMode: RoutePlaybackMode,
        bypassRegionCheck: Boolean = false,
    ): RouteRecomputeResult {
        if (!bypassRegionCheck) {
            val summary = computeRequiredRegions(points)
            if (!summary.isFullyDownloaded) return RouteRecomputeResult.MissingRegions(summary)
        }

        val options = prepareRouteOptions(points)
        val activeRoute =
            when (activeMode) {
                RoutePlaybackMode.GUIDED -> options.guided
                RoutePlaybackMode.FREE_ROAM -> options.freeRoam
            } ?: return RouteRecomputeResult.ActiveModeFailed("No viable path for $activeMode")

        val inactiveAvailability =
            if (activeMode == RoutePlaybackMode.FREE_ROAM) {
                resolveGuidedAvailabilityWithRetries(points, firstAttemptResult = options.guided)
            } else {
                ModeAvailability.AVAILABLE
            }

        return RouteRecomputeResult.Applied(activeRoute, inactiveAvailability)
    }

    private fun resolveGuidedAvailabilityWithRetries(
        points: List<RoutePoint>,
        firstAttemptResult: Route?,
    ): ModeAvailability {
        if (firstAttemptResult != null) return ModeAvailability.AVAILABLE
        repeat(INACTIVE_GUIDED_IMMEDIATE_RETRY_ATTEMPTS) {
            if (computeRoute(points) is Result.Success) return ModeAvailability.AVAILABLE
        }
        return ModeAvailability.UNAVAILABLE_PENDING_RETRY
    }
}
