package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.SimulationController
import kotlinx.coroutines.flow.SharedFlow

class ObserveAuthorizationLostUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke(): SharedFlow<Unit> = controller.authorizationLost
}
