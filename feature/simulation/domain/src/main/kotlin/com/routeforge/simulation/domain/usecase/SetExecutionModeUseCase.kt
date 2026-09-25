package com.routeforge.simulation.domain.usecase

import com.routeforge.coredomain.Result
import com.routeforge.simulation.domain.ExecutionModeFailure
import com.routeforge.simulation.domain.SimulationController
import com.routeforge.simulation.domain.model.ExecutionMode

class SetExecutionModeUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke(mode: ExecutionMode): Result<Unit, ExecutionModeFailure> = controller.setExecutionMode(mode)
}
