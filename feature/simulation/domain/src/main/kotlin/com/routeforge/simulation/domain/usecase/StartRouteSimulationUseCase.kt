package com.routeforge.simulation.domain.usecase

import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.SimulationController
import com.routeforge.simulation.domain.model.ExecutionMode
import com.routeforge.simulation.domain.model.SpeedSetting

class StartRouteSimulationUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke(
        route: Route,
        speedSetting: SpeedSetting,
        executionMode: ExecutionMode = ExecutionMode.DEFAULT,
    ) = controller.startRoute(route, speedSetting, executionMode)
}
