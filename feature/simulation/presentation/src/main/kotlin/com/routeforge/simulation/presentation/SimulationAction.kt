package com.routeforge.simulation.presentation

sealed interface SimulationAction {
    data class OnMapTap(
        val latitude: Double,
        val longitude: Double,
    ) : SimulationAction

    data object OnConfirmTeleport : SimulationAction

    data object OnCancelTeleport : SimulationAction

    data class OnSpeedInputChange(
        val value: String,
    ) : SimulationAction

    data object OnStartRouteSimulation : SimulationAction

    data object OnPauseSimulation : SimulationAction

    data object OnResumeSimulation : SimulationAction

    data object OnStopSimulation : SimulationAction

    data object OnCancelMockClick : SimulationAction

    data object OnConfirmCancelMock : SimulationAction

    data object OnDismissCancelMock : SimulationAction

    data object OnPlanRouteClick : SimulationAction

    data object OnOpenSetupClick : SimulationAction

    data object OnLocationPermissionGranted : SimulationAction
}
