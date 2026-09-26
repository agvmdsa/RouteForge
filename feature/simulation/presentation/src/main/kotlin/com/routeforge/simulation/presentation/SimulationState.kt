package com.routeforge.simulation.presentation

import com.routeforge.coredomain.model.RealLocation
import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.model.SimulationSession

enum class SimulationErrorType {
    NOT_AUTHORIZED,
    INVALID_SPEED,
    INVALID_EXECUTION_TIMES,
    JOYSTICK_NO_REAL_FIX,
    REAL_LOCATION_PROVIDER_DISABLED,
    REAL_LOCATION_PERMISSION_DENIED,
    REAL_LOCATION_TIMED_OUT,
}

enum class ExecutionModeSelection { ONCE, TIMES, LOOP }

data class SimulationState(
    val mockedSession: SimulationSession? = null,
    val realLocation: RealLocation? = null,
    val isSearchingRealLocation: Boolean = false,
    val pendingTeleportLatitude: Double? = null,
    val pendingTeleportLongitude: Double? = null,
    val isPendingTeleportBlockedOffline: Boolean = false,
    val isPendingCancelMock: Boolean = false,
    val loadedRoute: Route? = null,
    val playbackSpeedKmh: Float = 80f,
    val executionModeSelection: ExecutionModeSelection = ExecutionModeSelection.ONCE,
    val executionTimesInput: String = "2",
    val isStartRouteDialogOpen: Boolean = false,
    val isPendingCancelRoute: Boolean = false,
    val isJoystickVisible: Boolean = false,
    val joystickSpeedKmh: Float = 5f,
    val isJoystickInterruptPending: Boolean = false,
    val errorType: SimulationErrorType? = null,
    val isBlockedByAuthorization: Boolean = false,
)
