package com.routeforge.coredomain.usecase

import com.routeforge.coredomain.model.ModeAvailability
import com.routeforge.coredomain.model.RequiredRegionsSummary
import com.routeforge.coredomain.model.Route

/** Outcome of [RecomputeRouteForBothModesUseCase] — spec 008 FR-009/FR-010/FR-011. */
sealed interface RouteRecomputeResult {
    /** The active mode recomputed successfully and applies in full. [inactiveModeAvailability]
     *  reports whether the background inactive-mode recompute also succeeded (FR-011). */
    data class Applied(
        val activeRoute: Route,
        val inactiveModeAvailability: ModeAvailability,
    ) : RouteRecomputeResult

    /** FR-009's region check failed before either mode was attempted. */
    data class MissingRegions(
        val summary: RequiredRegionsSummary,
    ) : RouteRecomputeResult

    /** FR-010's gatekeeping failure — the caller must fully revert the edit that triggered this
     *  recompute and show an error; nothing about the route changes. */
    data class ActiveModeFailed(
        val reason: String,
    ) : RouteRecomputeResult
}
