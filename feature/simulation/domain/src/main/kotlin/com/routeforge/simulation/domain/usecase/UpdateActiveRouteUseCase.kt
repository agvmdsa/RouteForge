package com.routeforge.simulation.domain.usecase

import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.SimulationController

class UpdateActiveRouteUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke(route: Route) = controller.updateActiveRoute(route)
}
