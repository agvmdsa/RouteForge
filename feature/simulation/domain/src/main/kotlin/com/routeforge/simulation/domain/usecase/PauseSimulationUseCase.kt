package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.SimulationController

class PauseSimulationUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke() = controller.pause()
}
