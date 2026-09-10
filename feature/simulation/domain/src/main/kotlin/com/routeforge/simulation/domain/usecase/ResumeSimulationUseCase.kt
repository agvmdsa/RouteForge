package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.SimulationController

class ResumeSimulationUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke() = controller.resume()
}
