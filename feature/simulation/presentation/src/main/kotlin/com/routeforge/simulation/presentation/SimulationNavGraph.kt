package com.routeforge.simulation.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.simulationGraph(
    onOpenSetup: () -> Unit,
    onOpenRegionCatalog: () -> Unit,
) {
    composable<SimulationRoute> {
        SimulationRoot(onOpenSetup = onOpenSetup, onOpenRegionCatalog = onOpenRegionCatalog)
    }
}
