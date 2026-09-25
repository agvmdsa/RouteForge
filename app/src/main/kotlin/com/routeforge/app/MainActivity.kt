package com.routeforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.mocklocationsetup.domain.usecase.ObserveSetupStateUseCase
import com.routeforge.mocklocationsetup.presentation.MockLocationSetupRoute
import com.routeforge.mocklocationsetup.presentation.mockLocationSetupGraph
import com.routeforge.routing.presentation.RouteRequestRoute
import com.routeforge.routing.presentation.routingGraph
import com.routeforge.simulation.presentation.SimulationRoute
import com.routeforge.simulation.presentation.simulationGraph
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val lastComputedRouteHolder: LastComputedRouteHolder by inject()
    private val observeSetupState: ObserveSetupStateUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Skip the setup flow entirely on launch if it's already done — it should only ever be
        // seen again if something actually revokes mock-location access later (see
        // SimulationViewModel's resume-triggered re-check), never just because the app restarted.
        val startDestination = if (observeSetupState().isReady) SimulationRoute else MockLocationSetupRoute
        setContent {
            RouteForgeTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                ) {
                    mockLocationSetupGraph(
                        onSetupReady = {
                            navController.navigate(SimulationRoute) {
                                popUpTo(MockLocationSetupRoute) { inclusive = true }
                            }
                        },
                    )
                    simulationGraph(
                        onPlanRoute = { navController.navigate(RouteRequestRoute) },
                        onOpenSetup = { navController.navigate(MockLocationSetupRoute) },
                    )
                    routingGraph(
                        navController = navController,
                        onRouteComputed = { route ->
                            lastComputedRouteHolder.set(route)
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}
