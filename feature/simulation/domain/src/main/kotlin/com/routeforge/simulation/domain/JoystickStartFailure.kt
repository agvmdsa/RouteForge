package com.routeforge.simulation.domain

import com.routeforge.coredomain.Error

/** FR-026: the joystick's only failure case — no real GPS fix has ever been observed. */
sealed interface JoystickStartFailure : Error {
    data object NoRealLocationFixYet : JoystickStartFailure
}
