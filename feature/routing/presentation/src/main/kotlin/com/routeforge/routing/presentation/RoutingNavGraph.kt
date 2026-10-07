package com.routeforge.routing.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.routeforge.coredomain.model.Route
import com.routeforge.routing.presentation.favorites.FavoritesRoot
import com.routeforge.routing.presentation.favorites.FavoritesRoute
import com.routeforge.routing.presentation.regioncatalog.RegionCatalogRoot
import com.routeforge.routing.presentation.regioncatalog.RegionCatalogRoute
import com.routeforge.routing.presentation.regioncoveragemap.RegionCoverageMapRoot
import com.routeforge.routing.presentation.regioncoveragemap.RegionCoverageMapRoute
import com.routeforge.routing.presentation.routerequest.RouteRequestRoot
import com.routeforge.routing.presentation.routerequest.RouteRequestRoute
import com.routeforge.routing.presentation.savedroutes.SavedRoutesRoot
import com.routeforge.routing.presentation.savedroutes.SavedRoutesRoute
import com.routeforge.routing.presentation.settings.SettingsRoot
import com.routeforge.routing.presentation.settings.SettingsRoute

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
        RegionCatalogRoot(onOpenCoverageMap = { navController.navigate(RegionCoverageMapRoute) })
    }
    composable<RegionCoverageMapRoute> {
        RegionCoverageMapRoot()
    }
    composable<SettingsRoute> {
        SettingsRoot(onManageDownloadsClick = { navController.navigate(RegionCatalogRoute) })
    }
    composable<FavoritesRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<FavoritesRoute>()
        FavoritesRoot(isPickerMode = route.isPickerMode, onDone = { navController.popBackStack() })
    }
    composable<SavedRoutesRoute> {
        SavedRoutesRoot(
            onDone = { navController.popBackStack() },
            onOpenRegionCatalog = { navController.navigate(RegionCatalogRoute) },
        )
    }
}
