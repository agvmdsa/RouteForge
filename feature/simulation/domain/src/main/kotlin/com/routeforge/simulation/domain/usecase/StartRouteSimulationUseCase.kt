package com.routeforge.simulation.domain.usecase

import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.SimulationController

class StartRouteSimulationUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke(
        route: Route,
        speedMetersPerSecond: Float,
    ) = controller.startRoute(route, speedMetersPerSecond)
}
