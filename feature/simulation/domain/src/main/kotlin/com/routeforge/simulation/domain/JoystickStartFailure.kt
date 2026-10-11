package com.routeforge.simulation.domain

import com.routeforge.coredomain.DomainError

/** FR-026: the joystick's only failure case — no real GPS fix has ever been observed. */
sealed interface JoystickStartFailure : DomainError {
    data object NoRealLocationFixYet : JoystickStartFailure
}
