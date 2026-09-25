package com.routeforge.simulation.presentation

sealed interface SimulationAction {
    data class OnMapTap(
        val latitude: Double,
        val longitude: Double,
    ) : SimulationAction

    data object OnConfirmTeleport : SimulationAction

    data object OnCancelTeleport : SimulationAction

    data class OnPlaybackSpeedChange(
        val kmh: Float,
    ) : SimulationAction

    data class OnExecutionModeSelected(
        val selection: ExecutionModeSelection,
    ) : SimulationAction

    data class OnExecutionTimesInputChange(
        val value: String,
    ) : SimulationAction

    data object OnStartRouteSimulation : SimulationAction

    data object OnPauseSimulation : SimulationAction

    data object OnResumeSimulation : SimulationAction

    data object OnStopSimulation : SimulationAction

    data class OnJoystickDrag(
        val bearingDegrees: Float,
    ) : SimulationAction

    data object OnJoystickReleased : SimulationAction

    data object OnToggleJoystick : SimulationAction

    data class OnJoystickSpeedChange(
        val kmh: Float,
    ) : SimulationAction

    data object OnConfirmJoystickInterrupt : SimulationAction

    data object OnDismissJoystickInterrupt : SimulationAction

    data object OnCancelMockClick : SimulationAction

    data object OnConfirmCancelMock : SimulationAction

    data object OnDismissCancelMock : SimulationAction

    data object OnPlanRouteClick : SimulationAction

    data object OnOpenSetupClick : SimulationAction

    data object OnLocationPermissionGranted : SimulationAction
}
