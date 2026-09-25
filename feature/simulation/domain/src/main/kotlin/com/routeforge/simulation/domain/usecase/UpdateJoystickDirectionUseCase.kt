package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.SimulationController

class UpdateJoystickDirectionUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke(bearingDegrees: Float) = controller.updateJoystickDirection(bearingDegrees)
}
