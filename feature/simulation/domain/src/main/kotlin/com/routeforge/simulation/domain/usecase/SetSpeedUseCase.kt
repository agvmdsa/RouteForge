package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.SimulationController
import com.routeforge.simulation.domain.model.SpeedSetting

class SetSpeedUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke(speed: SpeedSetting) = controller.setSpeed(speed)
}
