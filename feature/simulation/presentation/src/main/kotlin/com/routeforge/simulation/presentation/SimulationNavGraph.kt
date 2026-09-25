package com.routeforge.simulation.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.simulationGraph(
    onPlanRoute: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    composable<SimulationRoute> {
        SimulationRoot(
            onPlanRoute = onPlanRoute,
            onOpenSetup = onOpenSetup,
        )
    }
}
