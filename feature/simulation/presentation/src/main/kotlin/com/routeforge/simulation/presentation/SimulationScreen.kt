package com.routeforge.simulation.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import com.routeforge.designsystem.components.NoOpTourProgressRepository
import com.routeforge.designsystem.components.TourHost
import com.routeforge.designsystem.components.TourLabels
import com.routeforge.designsystem.map.RouteForgeMap
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.designsystem.theme.Spacing
import com.routeforge.simulation.presentation.components.CancelMockConfirmationSheet
import com.routeforge.simulation.presentation.components.CancelRouteConfirmationSheet
import com.routeforge.simulation.presentation.components.FavoriteTeleportCancelRouteSheet
import com.routeforge.simulation.presentation.components.JoystickInterruptSheet
import com.routeforge.simulation.presentation.components.RouteProgressCard
import com.routeforge.simulation.presentation.components.StartRouteDialog
import com.routeforge.simulation.presentation.components.SwitchRouteModeSheet
import com.routeforge.simulation.presentation.components.TeleportConfirmationSheet
import com.routeforge.simulation.presentation.waypointedit.WaypointEditAction
import com.routeforge.simulation.presentation.waypointedit.WaypointEditEvent
import com.routeforge.simulation.presentation.waypointedit.WaypointEditState
import com.routeforge.simulation.presentation.waypointedit.WaypointEditViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
