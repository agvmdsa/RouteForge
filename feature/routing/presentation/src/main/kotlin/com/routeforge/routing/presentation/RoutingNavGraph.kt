package com.routeforge.routing.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.routeforge.routing.presentation.favorites.FavoritesRoot
import com.routeforge.routing.presentation.favorites.FavoritesRoute
import com.routeforge.routing.presentation.regioncatalog.RegionCatalogRoot
import com.routeforge.routing.presentation.regioncatalog.RegionCatalogRoute
import com.routeforge.routing.presentation.regioncoveragemap.RegionCoverageMapRoot
import com.routeforge.routing.presentation.regioncoveragemap.RegionCoverageMapRoute
import com.routeforge.routing.presentation.routerequest.RouteRequestRoot
import com.routeforge.routing.presentation.routerequest.RouteRequestRoute
import com.routeforge.routing.presentation.saved.SavedRoot
import com.routeforge.routing.presentation.saved.SavedRoute
import com.routeforge.routing.presentation.settings.SettingsRoot
import com.routeforge.routing.presentation.settings.SettingsRoute

/** The Plan Route tab's own nested graph: its root screen plus every sub-screen reachable from it. */
fun NavGraphBuilder.planRouteGraph(
    navController: NavController,
    onGoToSimulate: () -> Unit,
) {
    composable<RouteRequestRoute> {
        RouteRequestRoot(
            onGoToSimulate = onGoToSimulate,
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
    composable<FavoritesRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<FavoritesRoute>()
        FavoritesRoot(isPickerMode = route.isPickerMode, onDone = { navController.popBackStack() })
    }
}

/** The Saved tab's own nested graph. `RegionCatalogRoute`/`RegionCoverageMapRoute` are declared here
 *  too (an independent instance from [planRouteGraph]'s and [settingsGraph]'s) since using a saved
 *  route that needs regions not yet downloaded can open the catalog from within this tab as well. */
fun NavGraphBuilder.savedGraph(
    navController: NavController,
    onGoToSimulate: () -> Unit,
) {
    composable<SavedRoute> {
        SavedRoot(
            onGoToSimulate = onGoToSimulate,
            onOpenRegionCatalog = { navController.navigate(RegionCatalogRoute) },
        )
    }
    composable<RegionCatalogRoute> {
        RegionCatalogRoot(onOpenCoverageMap = { navController.navigate(RegionCoverageMapRoute) })
    }
    composable<RegionCoverageMapRoute> {
        RegionCoverageMapRoot()
    }
}

/** The Settings tab's own nested graph. */
fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable<SettingsRoute> {
        SettingsRoot(onManageDownloadsClick = { navController.navigate(RegionCatalogRoute) })
    }
    composable<RegionCatalogRoute> {
        RegionCatalogRoot(onOpenCoverageMap = { navController.navigate(RegionCoverageMapRoute) })
    }
    composable<RegionCoverageMapRoute> {
        RegionCoverageMapRoot()
    }
}
