package com.routeforge.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.routeforge.addresssearch.presentation.AddressSearchRoot
import com.routeforge.addresssearch.presentation.AddressSearchRoute
import com.routeforge.addresssearch.presentation.SearchSourceContext
import com.routeforge.addresssearch.presentation.initialPlace
import com.routeforge.coredomain.TourReplayCoordinator
import com.routeforge.coredomain.holder.PendingIncomingRouteHolder
import com.routeforge.coredomain.holder.PendingSearchWaypointHolder
import com.routeforge.coredomain.link.IncomingAction
import com.routeforge.coredomain.link.IncomingPlacement
import com.routeforge.coredomain.model.IncomingRoute
import com.routeforge.coredomain.model.PlaceSearchResult
import com.routeforge.coredomain.model.TourSection
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.mocklocationsetup.domain.usecase.ObserveSetupStateUseCase
import com.routeforge.mocklocationsetup.presentation.MockLocationSetupRoute
import com.routeforge.mocklocationsetup.presentation.mockLocationSetupGraph
import com.routeforge.routing.presentation.planRouteGraph
import com.routeforge.routing.presentation.regionCatalogGraph
import com.routeforge.routing.presentation.regioncatalog.RegionCatalogRoute
import com.routeforge.routing.presentation.routerequest.RouteRequestRoute
import com.routeforge.routing.presentation.saved.SavedRoute
import com.routeforge.routing.presentation.savedGraph
import com.routeforge.routing.presentation.settings.SettingsRoute
import com.routeforge.routing.presentation.settingsGraph
import com.routeforge.simulation.data.EXTRA_OPEN_SIMULATE
import com.routeforge.simulation.presentation.SimulationRoute
import com.routeforge.simulation.presentation.simulationGraph
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    private val observeSetupState: ObserveSetupStateUseCase by inject()
    private val incomingItems: IncomingItemsViewModel by viewModel()

    /** Set when an intent (initial launch or [onNewIntent]) carries [EXTRA_OPEN_SIMULATE] — e.g. tapping
     *  the simulation notification's body (FR-007) — so the composition can switch to the Simulate tab
     *  even if it was already running on a different one. A plain Activity property, not `remember`ed
     *  inside the composable, since [onNewIntent] fires outside recomposition. */
    private var pendingOpenSimulateRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // After a rebuild the system delivers the original intent again; only a fresh start reads it.
        if (savedInstanceState == null) IncomingIntentReader.read(intent)?.let(incomingItems::submit)
        handleIntent(intent)
        // Skip the setup flow entirely on launch if it's already done — it should only ever be
        // seen again if something actually revokes mock-location access later (see
        // SimulationViewModel's resume-triggered re-check), never just because the app restarted.
        val startDestination = if (observeSetupState().isReady) SimulateTabRoute else MockLocationSetupRoute
        setContent {
            RouteForgeTheme {
                val navController = rememberNavController()
                LaunchedEffect(pendingOpenSimulateRequest) {
                    if (pendingOpenSimulateRequest) {
                        navController.switchToTab(SimulateTabRoute)
                        pendingOpenSimulateRequest = false
                    }
                }
                BringReplayedTourTabForward(navController)
                HandleIncomingItems(navController, incomingItems)
                MainTabsScaffold(navController = navController) { paddingValues ->
                    val goToSimulate = { navController.switchToTab(SimulateTabRoute) }
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(paddingValues),
                    ) {
                        mockLocationSetupGraph(
                            onSetupReady = {
                                navController.navigate(SimulateTabRoute) {
                                    popUpTo(MockLocationSetupRoute) { inclusive = true }
                                }
                            },
                        )
                        navigation<SimulateTabRoute>(startDestination = SimulationRoute) {
                            simulationGraph(
                                onOpenSetup = { navController.navigate(MockLocationSetupRoute) },
                                onOpenRegionCatalog = { navController.navigate(RegionCatalogRoute) },
                            )
                            regionCatalogGraph(navController)
                        }
                        navigation<PlanTabRoute>(startDestination = RouteRequestRoute) {
                            planRouteGraph(
                                navController = navController,
                                onGoToSimulate = goToSimulate,
                            )
                        }
                        navigation<SavedTabRoute>(startDestination = SavedRoute) {
                            savedGraph(
                                navController = navController,
                                onGoToSimulate = goToSimulate,
                            )
                        }
                        navigation<SettingsTabRoute>(startDestination = SettingsRoute) {
                            settingsGraph(navController = navController)
                        }
                        composable<AddressSearchRoute> { backStackEntry ->
                            val route = backStackEntry.toRoute<AddressSearchRoute>()
                            AddressSearchRoot(
                                sourceContext = route.sourceContext,
                                referenceLatitude = route.referenceLatitude,
                                referenceLongitude = route.referenceLongitude,
                                initialPlace = route.initialPlace(),
                                initialQuery = route.initialQuery,
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        IncomingIntentReader.read(intent)?.let(incomingItems::submit)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_SIMULATE, false) == true) {
            pendingOpenSimulateRequest = true
        }
    }
}

/** Switches to the tab of whichever tour section Settings just asked to replay. Lives here, not in
 *  Settings, because "Repeat all" keeps moving on to the next section after the user has left Settings
 *  — and Settings can only navigate while it is composed. The section's own screen then picks the
 *  request up from the same coordinator once it is on screen. */
@Composable
private fun BringReplayedTourTabForward(navController: NavController) {
    val replayCoordinator: TourReplayCoordinator = koinInject()
    val pendingReplay by replayCoordinator.pendingReplay.collectAsState()
    LaunchedEffect(pendingReplay) {
        val tab =
            when (pendingReplay) {
                TourSection.SIMULATION -> SimulateTabRoute
                TourSection.ROUTE_CREATION -> PlanTabRoute
                null -> return@LaunchedEffect
            }
        navController.switchToTab(tab)
    }
}

/** Acts on what another app sent, once a tab is showing (so not during the first-run setup), and acts on it once. */
@Composable
private fun HandleIncomingItems(
    navController: NavController,
    incomingItems: IncomingItemsViewModel,
) {
    val pending by incomingItems.pending.collectAsState()
    val rootTab = navController.currentBackStackEntryAsState().value?.destination?.rootTab()
    val context = LocalContext.current
    val searchWaypointHolder: PendingSearchWaypointHolder = koinInject()
    val incomingRouteHolder: PendingIncomingRouteHolder = koinInject()
    LaunchedEffect(pending, rootTab) {
        if (pending == null || rootTab == null) return@LaunchedEffect
        val result = incomingItems.consume() ?: return@LaunchedEffect
        val onPlanRoute = rootTab == AppTab.PLAN_ROUTE
        when (result) {
            is IncomingResult.Outcome ->
                when (val action = IncomingPlacement.actionFor(result.outcome, onPlanRoute, result.wasAddress)) {
                    is IncomingAction.AddWaypoint ->
                        searchWaypointHolder.set(
                            PlaceSearchResult(action.name.orEmpty(), "", action.point.latitude, action.point.longitude, 0.0),
                        )
                    is IncomingAction.OfferChoice ->
                        navController.navigate(
                            AddressSearchRoute(
                                sourceContext = SearchSourceContext.TELEPORT_OR_FAVORITE,
                                initialLatitude = action.point.latitude,
                                initialLongitude = action.point.longitude,
                                initialName = action.name,
                            ),
                        )
                    IncomingAction.NoCoordinates -> Toast.makeText(context, R.string.external_no_coordinates, Toast.LENGTH_LONG).show()
                    IncomingAction.NotFound -> Toast.makeText(context, R.string.external_place_not_found, Toast.LENGTH_LONG).show()
                    is IncomingAction.OpenSearch -> {
                        val sourceContext =
                            if (action.onPlanRoute) SearchSourceContext.PLAN_ROUTE else SearchSourceContext.TELEPORT_OR_FAVORITE
                        navController.navigate(AddressSearchRoute(sourceContext = sourceContext, initialQuery = action.query))
                    }
                    is IncomingAction.LoadRoute -> {
                        incomingRouteHolder.set(IncomingRoute.Stops(action.stops))
                        navController.switchToTab(PlanTabRoute)
                    }
                }
            is IncomingResult.Route -> {
                incomingRouteHolder.set(result.route)
                navController.switchToTab(PlanTabRoute)
            }
            is IncomingResult.Message -> Toast.makeText(context, result.text, Toast.LENGTH_LONG).show()
        }
    }
}
