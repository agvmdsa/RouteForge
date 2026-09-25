package com.routeforge.simulation.domain.usecase

import com.routeforge.coredomain.Result
import com.routeforge.simulation.domain.JoystickStartFailure
import com.routeforge.simulation.domain.SimulationController

/** FR-025: stops the interrupted route and hands control to the joystick from its position. */
class ConfirmJoystickInterruptUseCase(
    private val controller: SimulationController,
) {
    operator fun invoke(): Result<Unit, JoystickStartFailure> = controller.startJoystick()
}
