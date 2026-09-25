package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.FreeRoamRouteBuilder
import com.routeforge.routing.domain.model.RouteOptions

/**
 * FR-001–FR-004: runs the real Guided computation and only exposes it as an option if it
 * succeeds for the full ordered waypoint list. Free-roam is always computed and always offered.
 */
class PrepareRouteOptionsUseCase(
    private val computeRoute: ComputeRouteUseCase,
    private val freeRoamRouteBuilder: FreeRoamRouteBuilder,
) {
    operator fun invoke(points: List<RoutePoint>): RouteOptions {
        val freeRoam = freeRoamRouteBuilder.build(points)
        val guided =
            when (val result = computeRoute(points)) {
                is Result.Success -> result.data.copy(mode = RoutePlaybackMode.GUIDED)
                is Result.Error -> null
            }
        return RouteOptions(guided = guided, freeRoam = freeRoam)
    }
}
