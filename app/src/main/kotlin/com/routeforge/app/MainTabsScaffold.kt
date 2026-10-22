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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.routeforge.designsystem.components.LocalTourTargetRegistry
import com.routeforge.designsystem.components.TourTargetRegistry
import com.routeforge.designsystem.components.TourTargets
import com.routeforge.designsystem.components.tourTarget
import com.routeforge.routing.presentation.routerequest.RouteRequestRoute
import com.routeforge.routing.presentation.saved.SavedRoute
import com.routeforge.routing.presentation.settings.SettingsRoute
import com.routeforge.simulation.presentation.SimulationRoute
import kotlin.reflect.KClass

/** Identifies a tab independently of its nav route type, so comparisons (e.g. "is the active tab
 *  Plan Route?") are a safe, exhaustive-checkable `==` on an enum rather than `===` on [TabItem.tabRoute],
 *  which only worked because every route happened to be a `data object` singleton — a non-obvious
 *  invariant a future parameterized route would silently break. */
internal enum class AppTab { SIMULATE, PLAN_ROUTE, SAVED, SETTINGS }

private class TabItem(
    val tab: AppTab,
    val tabRoute: TabRoute,
    val icon: ImageVector,
    val labelRes: Int,
    val isRootDestination: (NavDestination) -> Boolean,
    val isInHierarchy: (NavDestination) -> Boolean,
)

private fun isRootRoute(routeClass: KClass<*>): (NavDestination) -> Boolean = { it.hasRoute(routeClass) }

private fun isInTabHierarchy(tabRouteClass: KClass<*>): (NavDestination) -> Boolean =
    { it.hierarchy.any { destination -> destination.hasRoute(tabRouteClass) } }

/** One row of the bottom bar, in display order — a real tab, or the non-tab search action sitting
 *  between them. Rendering a single ordered list like this (rather than splicing the search item
 *  between `TabItems.take(2)`/`drop(2)`) means the search icon's position is explicit data, not an
 *  index convention a future 5th tab could silently misplace. */
private sealed interface NavBarEntry {
    data class Tab(
        val item: TabItem,
    ) : NavBarEntry

    data object Search : NavBarEntry
}

private val NavBarEntries: List<NavBarEntry> =
    listOf(
        NavBarEntry.Tab(
            TabItem(
                tab = AppTab.SIMULATE,
                tabRoute = SimulateTabRoute,
                icon = Icons.Filled.MyLocation,
                labelRes = R.string.tab_simulate_label,
                isRootDestination = isRootRoute(SimulationRoute::class),
                isInHierarchy = isInTabHierarchy(SimulateTabRoute::class),
            ),
        ),
        NavBarEntry.Tab(
            TabItem(
                tab = AppTab.PLAN_ROUTE,
                tabRoute = PlanTabRoute,
                icon = Icons.Filled.EditLocation,
                labelRes = R.string.tab_plan_route_label,
                isRootDestination = isRootRoute(RouteRequestRoute::class),
                isInHierarchy = isInTabHierarchy(PlanTabRoute::class),
            ),
        ),
        NavBarEntry.Search,
        NavBarEntry.Tab(
            TabItem(
                tab = AppTab.SAVED,
                tabRoute = SavedTabRoute,
                icon = Icons.Filled.Bookmark,
                labelRes = R.string.tab_saved_label,
                isRootDestination = isRootRoute(SavedRoute::class),
                isInHierarchy = isInTabHierarchy(SavedTabRoute::class),
            ),
        ),
        NavBarEntry.Tab(
            TabItem(
                tab = AppTab.SETTINGS,
                tabRoute = SettingsTabRoute,
                icon = Icons.Filled.Settings,
                labelRes = R.string.tab_settings_label,
                isRootDestination = isRootRoute(SettingsRoute::class),
                isInHierarchy = isInTabHierarchy(SettingsTabRoute::class),
            ),
        ),
    )

private val Tabs: List<TabItem> = NavBarEntries.filterIsInstance<NavBarEntry.Tab>().map { it.item }

/** The tab whose own root screen is showing, or `null` on a pushed screen and during the first-run setup. */
internal fun NavDestination.rootTab(): AppTab? = Tabs.firstOrNull { it.isRootDestination(this) }?.tab

/** Navigates to [tabRoute] the way every bottom-tab-bar implementation should (research.md Decision 1
 *  for spec 007): pops back to the graph's start with its state saved, then restores the target tab's
 *  own saved state — so each tab remembers exactly where the user left it. */
internal fun NavController.switchToTab(tabRoute: TabRoute) {
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
internal fun MainTabsScaffold(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit,
) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val activeRootTab = currentDestination?.let { destination -> Tabs.firstOrNull { it.isRootDestination(destination) } }

    BackHandler(enabled = activeRootTab != null && activeRootTab.tab != AppTab.SIMULATE) {
        navController.switchToTab(SimulateTabRoute)
    }

    // The bottom bar belongs to this shell, outside every screen's own tour. Controls here that a screen's
    // tour wants to point at report into this registry, which each screen's tour falls back to.
    val shellTourTargets = remember { TourTargetRegistry() }
    CompositionLocalProvider(LocalTourTargetRegistry provides shellTourTargets) {
        Scaffold(
            bottomBar = {
                if (activeRootTab != null) {
                    // FR-013: no explicit insets handling needed here — ShortNavigationBar already
                    // applies WindowInsets.systemBarsForVisualComponents (horizontal + bottom) as its
                    // own default windowInsets param, which correctly reports a slimmer inset under
                    // gesture navigation and a taller one under 3-button navigation. Adding another
                    // windowInsetsPadding around this would double-count that inset.
                    ShortNavigationBar {
                        NavBarEntries.forEach { entry ->
                            when (entry) {
                                is NavBarEntry.Tab ->
                                    ShortNavigationBarItem(
                                        selected = currentDestination != null && entry.item.isInHierarchy(currentDestination),
                                        onClick = { navController.switchToTab(entry.item.tabRoute) },
                                        icon = { Icon(entry.item.icon, contentDescription = null) },
                                        label = { Text(stringResource(entry.item.labelRes)) },
                                        modifier =
                                            if (entry.item.tab == AppTab.SAVED) {
                                                Modifier.tourTarget(TourTargets.SAVED_TAB)
                                            } else {
                                                Modifier
                                            },
                                    )
                                NavBarEntry.Search ->
                                    ShortNavigationBarItem(
                                        selected = false,
                                        onClick = {
                                            val sourceContext =
                                                if (activeRootTab?.tab ==
                                                    AppTab.PLAN_ROUTE
                                                ) {
                                                    SearchSourceContext.PLAN_ROUTE
                                                } else {
                                                    SearchSourceContext.TELEPORT_OR_FAVORITE
                                                }
                                            navController.navigate(AddressSearchRoute(sourceContext = sourceContext))
                                        },
                                        icon = {
                                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.tab_search_label))
                                        },
                                        label = { Text(stringResource(R.string.tab_search_label)) },
                                    )
                            }
                        }
                    }
                }
            },
        ) { paddingValues -> content(paddingValues) }
    }
}
