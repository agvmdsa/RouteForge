package com.routeforge.simulation.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.simulationGraph(
    navController: NavController,
    onPlanRoute: () -> Unit,
) {
    composable<SimulationRoute> {
        SimulationRoot(
            onPlanRoute = onPlanRoute,
            onOpenSetup = { navController.popBackStack() },
        )
    }
}
