package com.routeforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.mocklocationsetup.presentation.MockLocationSetupRoute
import com.routeforge.mocklocationsetup.presentation.mockLocationSetupGraph
import com.routeforge.routing.presentation.RouteRequestRoute
import com.routeforge.routing.presentation.routingGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RouteForgeTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = MockLocationSetupRoute,
                ) {
                    mockLocationSetupGraph(
                        navController = navController,
                        onSetupReady = { navController.navigate(RouteRequestRoute) },
                    )
                    routingGraph(
                        navController = navController,
                        onRouteComputed = {},
                    )
                }
            }
        }
    }
}
