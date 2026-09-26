package com.routeforge.simulation.presentation

sealed interface SimulationEvent {
    data object NavigateToPlanRoute : SimulationEvent

    data object NavigateToSetup : SimulationEvent

    data object NavigateToSettings : SimulationEvent
}
