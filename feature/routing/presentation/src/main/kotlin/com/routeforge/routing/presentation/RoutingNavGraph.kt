package com.routeforge.routing.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.routeforge.coredomain.model.Route

fun NavGraphBuilder.routingGraph(
    navController: NavController,
    onRouteComputed: (Route) -> Unit,
) {
    composable<RouteRequestRoute> {
        RouteRequestRoot(
            onRouteComputed = onRouteComputed,
            onOpenRegionCatalog = { navController.navigate(RegionCatalogRoute) },
            onOpenFavorites = { navController.navigate(FavoritesRoute(isPickerMode = true)) },
        )
    }
    composable<RegionCatalogRoute> {
        RegionCatalogRoot()
    }
    composable<SettingsRoute> {
        SettingsRoot(onManageDownloadsClick = { navController.navigate(RegionCatalogRoute) })
    }
    composable<FavoritesRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<FavoritesRoute>()
        FavoritesRoot(isPickerMode = route.isPickerMode, onDone = { navController.popBackStack() })
    }
}
