package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.SimulationController
import com.routeforge.simulation.domain.model.JoystickInterruptDecision
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationStatus

/** FR-024: a route actively RUNNING or PAUSED requires confirmation before the joystick engages. */
class RequestJoystickInterruptUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke(): JoystickInterruptDecision {
        val session = controller.mockedSession.value
        val isInterruptingActiveRoute =
            session != null &&
                session.mode == SimulationMode.ROUTE &&
                (session.status == SimulationStatus.RUNNING || session.status == SimulationStatus.PAUSED)
        return if (isInterruptingActiveRoute) {
            JoystickInterruptDecision.ConfirmationRequired
        } else {
            JoystickInterruptDecision.ProceedImmediately
        }
    }
}
