package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.SimulationController

class TeleportUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke(
        latitude: Double,
        longitude: Double,
    ) = controller.teleport(latitude, longitude)
}
