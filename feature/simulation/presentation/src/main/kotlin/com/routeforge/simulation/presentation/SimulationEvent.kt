package com.routeforge.simulation.presentation

internal sealed interface SimulationEvent {
    data object NavigateToSetup : SimulationEvent
}
