package com.routeforge.routing.domain.model

import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode

/**
 * Output of [com.routeforge.routing.domain.usecase.PrepareRouteOptionsUseCase]. [freeRoam] is
 * always present; [guided] is present only if a real-road path was fully computable (FR-001).
 * Never both null.
 */
data class RouteOptions(
    val guided: Route?,
    val freeRoam: Route,
)

/** The Guided-preferred-else-Free-roam fallback rule (Constitution VI), in one place: non-null
 *  only when Guided isn't viable, giving the (route, mode) pair callers should resolve with
 *  immediately, without asking the user to choose. Null means both modes are viable and the
 *  caller must show a choice. */
val RouteOptions.autoResolved: Pair<Route, RoutePlaybackMode>?
    get() = if (guided == null) freeRoam to RoutePlaybackMode.FREE_ROAM else null

