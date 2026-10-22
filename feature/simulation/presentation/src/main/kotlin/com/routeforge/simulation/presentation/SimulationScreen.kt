package com.routeforge.simulation.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.coredomain.TourProgressRepository
import com.routeforge.coredomain.TourReplayCoordinator
import com.routeforge.coredomain.model.TourSection
import com.routeforge.designsystem.components.AutoDismissConfirmationPill
import com.routeforge.designsystem.components.FabColumn
import com.routeforge.designsystem.components.NoOpTourProgressRepository
import com.routeforge.designsystem.components.TooltipAnchor
import com.routeforge.designsystem.components.TopBanner
import com.routeforge.designsystem.components.TourHost
import com.routeforge.designsystem.components.TourLabels
import com.routeforge.designsystem.components.tourTarget
import com.routeforge.designsystem.map.RouteForgeMap
import com.routeforge.designsystem.speed.SpeedSelectorDialog
import com.routeforge.designsystem.speed.SpeedSelectorFab
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.designsystem.theme.Spacing
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.presentation.components.CancelMockConfirmationSheet
import com.routeforge.simulation.presentation.components.CancelRouteConfirmationSheet
import com.routeforge.simulation.presentation.components.CoordinatePill
import com.routeforge.simulation.presentation.components.FavoriteTeleportCancelRouteSheet
import com.routeforge.simulation.presentation.components.Joystick
import com.routeforge.simulation.presentation.components.JoystickInterruptSheet
import com.routeforge.simulation.presentation.components.RouteProgressCard
import com.routeforge.simulation.presentation.components.RouteProgressUi
import com.routeforge.simulation.presentation.components.StartRouteDialog
import com.routeforge.simulation.presentation.components.SwitchRouteModeSheet
import com.routeforge.simulation.presentation.components.TeleportConfirmationSheet
import com.routeforge.simulation.presentation.waypointedit.WaypointEditAction
import com.routeforge.simulation.presentation.waypointedit.WaypointEditEvent
import com.routeforge.simulation.presentation.waypointedit.WaypointEditState
import com.routeforge.simulation.presentation.waypointedit.WaypointEditViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private const val MIN_SPEED_KMH = 0f
private const val MAX_SPEED_KMH = 150f
private val SpeedSelectorRangeKmh = MIN_SPEED_KMH..MAX_SPEED_KMH

@Composable
internal fun SimulationRoot(
    onOpenSetup: () -> Unit,
    onOpenRegionCatalog: () -> Unit,
    viewModel: SimulationViewModel = koinViewModel(),
    waypointEditViewModel: WaypointEditViewModel = koinViewModel(),
    tourProgressRepository: TourProgressRepository = koinInject(),
    tourReplayCoordinator: TourReplayCoordinator = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editState by waypointEditViewModel.state.collectAsStateWithLifecycle()
    var showWaypointsUpdatedConfirmation by remember { mutableStateOf(false) }

    val runtimePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val locationGranted =
                results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (locationGranted) {
                viewModel.onAction(SimulationAction.OnLocationPermissionGranted)
            }
        }

    LaunchedEffect(Unit) {
        val requiredPermissions =
            buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        runtimePermissionLauncher.launch(requiredPermissions.toTypedArray())
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SimulationEvent.NavigateToSetup -> onOpenSetup()
            }
        }
    }

    LaunchedEffect(waypointEditViewModel) {
        waypointEditViewModel.events.collect { event ->
            when (event) {
                is WaypointEditEvent.Committed -> viewModel.applyEditedRoute(event.activeRoute)
                WaypointEditEvent.GoToDownloads -> onOpenRegionCatalog()
                WaypointEditEvent.AutoPauseRequested -> viewModel.onAction(SimulationAction.OnPauseSimulation)
                WaypointEditEvent.WaypointsUpdated -> showWaypointsUpdatedConfirmation = true
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(SimulationAction.OnScreenResumed)
    }

    SimulationScreen(
        state = state,
        onAction = viewModel::onAction,
        editState = editState,
        onEditAction = waypointEditViewModel::onAction,
        showWaypointsUpdatedConfirmation = showWaypointsUpdatedConfirmation,
        onDismissWaypointsUpdatedConfirmation = { showWaypointsUpdatedConfirmation = false },
        tourProgressRepository = tourProgressRepository,
        tourReplayCoordinator = tourReplayCoordinator,
    )
}

@Composable
internal fun SimulationScreen(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
    editState: WaypointEditState = WaypointEditState(),
    onEditAction: (WaypointEditAction) -> Unit = {},
    showWaypointsUpdatedConfirmation: Boolean = false,
    onDismissWaypointsUpdatedConfirmation: () -> Unit = {},
    tourProgressRepository: TourProgressRepository = remember { NoOpTourProgressRepository() },
    tourReplayCoordinator: TourReplayCoordinator = remember { TourReplayCoordinator() },
) {
    val cameraTarget = rememberSimulationCameraTarget(state)
    val mapContent = rememberSimulationMapContent(state, editState, onEditAction)
    val routeProgress = rememberRouteProgress(state)

    Scaffold { paddingValues ->
        TourHost(
            section = TourSection.SIMULATION,
            steps = rememberSimulationTourSteps(state),
            progressRepository = tourProgressRepository,
            replayCoordinator = tourReplayCoordinator,
            labels =
                TourLabels(
                    skip = stringResource(R.string.simulation_tour_skip_button),
                    next = stringResource(R.string.simulation_tour_next_button),
                    done = stringResource(R.string.simulation_tour_done_button),
                ),
            isTopMessageVisible = state.isBlockedByAuthorization || state.errorType != null || showWaypointsUpdatedConfirmation,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            RouteForgeMap(
                markers = mapContent.markers,
                polylinePoints = mapContent.polylinePoints,
                traveledPolylinePoints = mapContent.traveledPolylinePoints,
                cameraTarget = cameraTarget,
                mapStyle = state.mapStyle,
                onMapTap = { latitude, longitude ->
                    if (editState.isOpen) {
                        onEditAction(WaypointEditAction.OnMapTap(latitude, longitude))
                    } else if (state.loadedRoute == null) {
                        // Tap-to-teleport only makes sense with no route loaded — teleporting
                        // mid-route would fight the route's own playback position.
                        onAction(SimulationAction.OnMapTap(latitude, longitude))
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            SimulationTopOverlays(
                state = state,
                showWaypointsUpdatedConfirmation = showWaypointsUpdatedConfirmation,
                onDismissWaypointsUpdatedConfirmation = onDismissWaypointsUpdatedConfirmation,
                onAction = onAction,
            )

            routeProgress?.let {
                RouteProgressCard(
                    progress = it,
                    state = state,
                    sessionDistanceTraveledMeters = mapContent.sessionDistanceTraveledMeters,
                    editState = editState,
                    onEditAction = onEditAction,
                    onAction = onAction,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(Spacing.ScreenContent),
                )
            }

            SimulationMovementControls(state = state, onAction = onAction)
        }
    }

    WaypointEditOverlays(loadedRoute = state.loadedRoute, editState = editState, onEditAction = onEditAction)

    SimulationDialogs(state = state, onAction = onAction)
}

/** Non-null as soon as a route is loaded, not only once playback starts — the card hosts the
 *  Cancel/Switch-mode/Waypoint-edit/Speed controls now (spec 011), which must be reachable the
 *  moment a route is loaded, same as the old fixed FABs they replaced. Before a mocked session
 *  exists yet (route loaded, not yet started), traveled distance is 0 and ETA is unknown — not an
 *  error, just nothing to report yet. Null whenever there's truly nothing to show progress for: no
 *  route loaded, a non-ROUTE session active (joystick/stationary have no "total distance" to
 *  measure against), or a route with no measurable length. Recomputed from [SimulationState] alone
 *  — no new domain calculation needed, the route's total distance and the session's traveled
 *  distance/speed (when a session exists) already carry everything this derives. */
@Composable
private fun rememberRouteProgress(state: SimulationState): RouteProgressUi? {
    val session = state.mockedSession
    val route = state.loadedRoute
    if (route == null || route.distanceMeters <= 0.0 || (session != null && session.mode != SimulationMode.ROUTE)) {
        return null
    }
    return remember(session?.distanceTraveledMeters, route.distanceMeters, session?.speedMetersPerSecond) {
        val traveled = (session?.distanceTraveledMeters ?: 0.0).coerceIn(0.0, route.distanceMeters)
        RouteProgressUi(
            fractionComplete = (traveled / route.distanceMeters).toFloat(),
            traveledMeters = traveled,
            totalMeters = route.distanceMeters,
            etaSeconds =
                if (session != null && session.speedMetersPerSecond > 0f) {
                    (route.distanceMeters - traveled) / session.speedMetersPerSecond
                } else {
                    null
                },
        )
    }
}

/** Top-anchored overlays: the coordinate pill, the authorization/error banner, and the transient
 *  "waypoints updated" confirmation. */
@Composable
private fun BoxScope.SimulationTopOverlays(
    state: SimulationState,
    showWaypointsUpdatedConfirmation: Boolean,
    onDismissWaypointsUpdatedConfirmation: () -> Unit,
    onAction: (SimulationAction) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = Spacing.ScreenContent, start = Spacing.ScreenContent, end = Spacing.ScreenContent),
        contentAlignment = Alignment.Center,
    ) {
        CoordinatePill(
            mockedSession = state.mockedSession,
            isSearchingRealLocation = state.isSearchingRealLocation,
            hasKnownRealLocation = state.realLocation != null,
            onCancelMockClick = { onAction(SimulationAction.OnCancelMockClick) },
        )
    }

    if (state.isBlockedByAuthorization) {
        TopBanner(
            contentPadding = Spacing.ScreenContent,
            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter,
                    ).padding(top = Spacing.BannerTopOffset, start = Spacing.ScreenContent, end = Spacing.ScreenContent),
        ) {
            Text(stringResource(R.string.simulation_error_not_authorized))
            Button(onClick = { onAction(SimulationAction.OnOpenSetupClick) }) {
                Text(stringResource(R.string.simulation_go_to_setup_button))
            }
        }
    } else {
        state.errorType?.let { errorType ->
            TopBanner(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopCenter,
                        ).padding(top = Spacing.BannerTopOffset, start = Spacing.ScreenContent, end = Spacing.ScreenContent),
            ) {
                Text(text = errorType.toMessage())
            }
        }
    }

    AutoDismissConfirmationPill(
        message = stringResource(R.string.simulation_waypoints_updated_message),
        isVisible = showWaypointsUpdatedConfirmation,
        onDismiss = onDismissWaypointsUpdatedConfirmation,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = Spacing.BannerTopOffset),
    )
}

/** Bottom-start FAB column (joystick speed selector, joystick toggle) plus the joystick itself and
 *  its speed dialog — grouped together since the dialog's open/closed state is only ever toggled
 *  from this column's own FAB. Route playback no longer has controls here (spec 011) — the
 *  joystick and an active route remain mutually exclusive, so this column is only ever shown when
 *  no route is loaded. */
@Composable
private fun BoxScope.SimulationMovementControls(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
) {
    if (state.loadedRoute != null) return
    var isJoystickSpeedDialogOpen by remember { mutableStateOf(false) }
    FabColumn(alignment = Alignment.BottomStart) {
        if (state.isJoystickVisible) {
            val joystickSpeedDescription = stringResource(R.string.simulation_joystick_speed_button)
            TooltipAnchor(tooltipText = joystickSpeedDescription) {
                SpeedSelectorFab(
                    onClick = { isJoystickSpeedDialogOpen = true },
                    contentDescription = joystickSpeedDescription,
                    modifier = Modifier.tourTarget("sim_joystick_speed"),
                )
            }
        }
        val joystickToggleDescription = stringResource(R.string.simulation_joystick_label)
        TooltipAnchor(tooltipText = joystickToggleDescription) {
            FloatingActionButton(
                onClick = { onAction(SimulationAction.OnToggleJoystick) },
                modifier = Modifier.tourTarget("sim_joystick_toggle"),
            ) {
                Icon(Icons.Filled.SportsEsports, contentDescription = joystickToggleDescription)
            }
        }
    }

    if (state.isJoystickVisible) {
        Joystick(
            onDrag = { bearing -> onAction(SimulationAction.OnJoystickDrag(bearing)) },
            onReleased = { onAction(SimulationAction.OnJoystickReleased) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.ScreenContent),
        )
        if (isJoystickSpeedDialogOpen) {
            SpeedSelectorDialog(
                title = stringResource(R.string.simulation_joystick_speed_dialog_title),
                speedLabel = stringResource(R.string.simulation_speed_value_label, state.joystickSpeedKmh.roundToInt()),
                confirmButtonLabel = stringResource(R.string.simulation_speed_dialog_close),
                speedKmh = state.joystickSpeedKmh,
                onSpeedChange = { kmh -> onAction(SimulationAction.OnJoystickSpeedChange(kmh)) },
                onDismiss = { isJoystickSpeedDialogOpen = false },
                speedRangeKmh = SpeedSelectorRangeKmh,
            )
        }
    }
}

/** The screen's modal sheets/dialogs — each gated on its own nullable/boolean state field, shown
 *  at most one at a time in practice since they correspond to mutually-exclusive user flows. */
@Composable
private fun SimulationDialogs(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
) {
    val pendingLatitude = state.pendingTeleportLatitude
    val pendingLongitude = state.pendingTeleportLongitude
    if (pendingLatitude != null && pendingLongitude != null) {
        TeleportConfirmationSheet(
            latitude = pendingLatitude,
            longitude = pendingLongitude,
            isBlockedOffline = state.isPendingTeleportBlockedOffline,
            onConfirm = { onAction(SimulationAction.OnConfirmTeleport) },
            onDismiss = { onAction(SimulationAction.OnCancelTeleport) },
        )
    }

    if (state.isPendingCancelMock) {
        CancelMockConfirmationSheet(
            onConfirm = { onAction(SimulationAction.OnConfirmCancelMock) },
            onDismiss = { onAction(SimulationAction.OnDismissCancelMock) },
        )
    }

    if (state.pendingFavoriteTeleportTarget != null) {
        FavoriteTeleportCancelRouteSheet(
            onConfirm = { onAction(SimulationAction.OnConfirmFavoriteTeleportCancelRoute) },
            onDismiss = { onAction(SimulationAction.OnDismissFavoriteTeleportCancelRoute) },
        )
    }

    val pendingRouteModeSwitch = state.pendingRouteModeSwitch
    if (pendingRouteModeSwitch != null) {
        SwitchRouteModeSheet(
            targetMode = pendingRouteModeSwitch,
            onConfirm = { onAction(SimulationAction.OnConfirmSwitchRouteMode) },
            onDismiss = { onAction(SimulationAction.OnDismissSwitchRouteMode) },
        )
    }

    if (state.isJoystickInterruptPending) {
        JoystickInterruptSheet(
            onConfirm = { onAction(SimulationAction.OnConfirmJoystickInterrupt) },
            onDismiss = { onAction(SimulationAction.OnDismissJoystickInterrupt) },
        )
    }

    if (state.isPendingCancelRoute) {
        CancelRouteConfirmationSheet(
            onConfirm = { onAction(SimulationAction.OnStopSimulation) },
            onDismiss = { onAction(SimulationAction.OnDismissCancelRoute) },
        )
    }

    if (state.isStartRouteDialogOpen) {
        StartRouteDialog(
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun SimulationErrorType.toMessage(): String =
    when (this) {
        SimulationErrorType.NOT_AUTHORIZED -> stringResource(R.string.simulation_error_not_authorized)
        SimulationErrorType.INVALID_SPEED -> stringResource(R.string.simulation_error_invalid_speed)
        SimulationErrorType.INVALID_EXECUTION_TIMES -> stringResource(R.string.simulation_error_invalid_execution_times)
        SimulationErrorType.JOYSTICK_NO_REAL_FIX -> stringResource(R.string.simulation_joystick_no_fix_message)
        SimulationErrorType.REAL_LOCATION_PROVIDER_DISABLED -> stringResource(R.string.simulation_error_real_location_provider_disabled)
        SimulationErrorType.REAL_LOCATION_PERMISSION_DENIED -> stringResource(R.string.simulation_error_real_location_permission_denied)
        SimulationErrorType.REAL_LOCATION_TIMED_OUT -> stringResource(R.string.simulation_error_real_location_timed_out)
    }

@Preview
@Composable
private fun SimulationScreenIdlePreview() {
    RouteForgeTheme {
        SimulationScreen(state = SimulationState(), onAction = {})
    }
}

@Preview
@Composable
private fun SimulationScreenBlockedPreview() {
    RouteForgeTheme {
        SimulationScreen(
            state =
                SimulationState(
                    isBlockedByAuthorization = true,
                    errorType = SimulationErrorType.NOT_AUTHORIZED,
                ),
            onAction = {},
        )
    }
}
