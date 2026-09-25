package com.routeforge.routing.domain.model

import com.routeforge.coredomain.model.Route

/**
 * Output of [com.routeforge.routing.domain.usecase.PrepareRouteOptionsUseCase]. [freeRoam] is
 * always present; [guided] is present only if a real-road path was fully computable (FR-001).
 * Never both null.
 */
data class RouteOptions(
    val guided: Route?,
    val freeRoam: Route,
)
