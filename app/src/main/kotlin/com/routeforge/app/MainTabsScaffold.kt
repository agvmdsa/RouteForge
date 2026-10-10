package com.routeforge.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.routeforge.addresssearch.presentation.AddressSearchRoute
import com.routeforge.addresssearch.presentation.SearchSourceContext
import com.routeforge.routing.presentation.routerequest.RouteRequestRoute
import com.routeforge.routing.presentation.saved.SavedRoute
import com.routeforge.routing.presentation.settings.SettingsRoute
import com.routeforge.simulation.presentation.SimulationRoute

private class TabItem(
    val tabRoute: Any,
    val icon: ImageVector,
    val labelRes: Int,
    val isRootDestination: (NavDestination) -> Boolean,
    val isInHierarchy: (NavDestination) -> Boolean,
)

private val TabItems =
    listOf(
        TabItem(
            tabRoute = SimulateTabRoute,
            icon = Icons.Filled.MyLocation,
            labelRes = R.string.tab_simulate_label,
            isRootDestination = { it.hasRoute(SimulationRoute::class) },
            isInHierarchy = { it.hierarchy.any { d -> d.hasRoute(SimulateTabRoute::class) } },
        ),
        TabItem(
            tabRoute = PlanRouteTabRoute,
            icon = Icons.Filled.EditLocation,
            labelRes = R.string.tab_plan_route_label,
            isRootDestination = { it.hasRoute(RouteRequestRoute::class) },
            isInHierarchy = { it.hierarchy.any { d -> d.hasRoute(PlanRouteTabRoute::class) } },
        ),
        TabItem(
            tabRoute = SavedTabRoute,
            icon = Icons.Filled.Bookmark,
            labelRes = R.string.tab_saved_label,
            isRootDestination = { it.hasRoute(SavedRoute::class) },
            isInHierarchy = { it.hierarchy.any { d -> d.hasRoute(SavedTabRoute::class) } },
        ),
        TabItem(
            tabRoute = SettingsTabRoute,
            icon = Icons.Filled.Settings,
            labelRes = R.string.tab_settings_label,
            isRootDestination = { it.hasRoute(SettingsRoute::class) },
            isInHierarchy = { it.hierarchy.any { d -> d.hasRoute(SettingsTabRoute::class) } },
        ),
    )

/** Navigates to [tabRoute] the way every bottom-tab-bar implementation should (research.md Decision 1
 *  for spec 007): pops back to the graph's start with its state saved, then restores the target tab's
 *  own saved state — so each tab remembers exactly where the user left it. */
internal fun NavController.switchToTab(tabRoute: Any) {
    navigate(tabRoute) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Wraps the tab-shell [content] in a [Scaffold] with a bottom [ShortNavigationBar] (the Material3
 *  "short" variant — 64dp tall instead of the standard 80dp "tall" bar, since the full-height one
 *  felt oversized for just four icon+label items) — visible only when the current destination is
 *  exactly one of the four tabs' own root screens (FR-006/FR-007: hidden on every pushed sub-screen
 *  and during first-run setup, since neither is ever one of these root routes).
 *
 *  Also implements FR-012: pressing back while on a non-Simulate tab's own root switches to the
 *  Simulate tab instead of exiting the app — the same [switchToTab] call a manual tab tap performs, so
 *  "back" and "tap Simulate" are the same mechanism, not two separate code paths. The [BackHandler] is
 *  only enabled in that exact situation; everywhere else (a pushed sub-screen, or Simulate's own root),
 *  it stays disabled and the platform default back behavior proceeds unmodified. */
@Composable
fun MainTabsScaffold(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit,
) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val activeRootTab = currentDestination?.let { destination -> TabItems.firstOrNull { it.isRootDestination(destination) } }

    BackHandler(enabled = activeRootTab != null && activeRootTab.tabRoute !== SimulateTabRoute) {
        navController.switchToTab(SimulateTabRoute)
    }

    Scaffold(
        bottomBar = {
            if (activeRootTab != null) {
                // FR-013: no explicit insets handling needed here — ShortNavigationBar already
                // applies WindowInsets.systemBarsForVisualComponents (horizontal + bottom) as its
                // own default windowInsets param, which correctly reports a slimmer inset under
                // gesture navigation and a taller one under 3-button navigation. Adding another
                // windowInsetsPadding around this would double-count that inset.
                ShortNavigationBar {
                    TabItems.take(2).forEach { tab ->
                        ShortNavigationBarItem(
                            selected = currentDestination != null && tab.isInHierarchy(currentDestination),
                            onClick = { navController.switchToTab(tab.tabRoute) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                    ShortNavigationBarItem(
                        selected = false,
                        onClick = {
                            val sourceContext =
                                if (activeRootTab?.tabRoute === PlanRouteTabRoute) SearchSourceContext.PLAN_ROUTE else SearchSourceContext.OTHER
                            navController.navigate(AddressSearchRoute(sourceContext = sourceContext))
                        },
                        icon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.tab_search_label)) },
                        label = { Text(stringResource(R.string.tab_search_label)) },
                    )
                    TabItems.drop(2).forEach { tab ->
                        ShortNavigationBarItem(
                            selected = currentDestination != null && tab.isInHierarchy(currentDestination),
                            onClick = { navController.switchToTab(tab.tabRoute) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { paddingValues -> content(paddingValues) }
}
