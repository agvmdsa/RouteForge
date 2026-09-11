package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.SimulationController
import com.routeforge.simulation.domain.model.SimulationSession
import kotlinx.coroutines.flow.StateFlow

class ObserveMockedSessionUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke(): StateFlow<SimulationSession?> = controller.mockedSession
}
