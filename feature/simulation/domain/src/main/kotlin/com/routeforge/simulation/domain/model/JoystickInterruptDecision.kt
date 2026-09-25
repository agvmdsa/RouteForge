package com.routeforge.simulation.domain.model

/** FR-024: whether engaging the joystick needs user confirmation first (a route is actively playing). */
sealed interface JoystickInterruptDecision {
    data object ProceedImmediately : JoystickInterruptDecision

    data object ConfirmationRequired : JoystickInterruptDecision
}
