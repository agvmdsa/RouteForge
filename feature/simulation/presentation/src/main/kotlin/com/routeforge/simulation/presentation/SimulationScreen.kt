package com.routeforge.simulation.presentation

import android.Manifest
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.designsystem.components.TopBanner
import com.routeforge.designsystem.map.RouteForgeMap
import com.routeforge.designsystem.map.RouteForgeMapMarker
import com.routeforge.designsystem.map.RouteForgeMapMarkerIcon
import com.routeforge.designsystem.speed.SpeedSelectorDialog
import com.routeforge.designsystem.speed.SpeedSelectorFab
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.simulation.domain.RouteProgressCalculator
import com.routeforge.simulation.domain.model.SimulationMode
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel

private val ScreenContentPadding = 16.dp
private val ControlsRowSpacing = 8.dp
private val JoystickBottomPadding = 24.dp
private val BannerTopOffset = 72.dp
private const val MIN_SPEED_KMH = 0f
private const val MAX_SPEED_KMH = 150f
private val SpeedSelectorRangeKmh = MIN_SPEED_KMH..MAX_SPEED_KMH

@Composable
fun SimulationRoot(
    onPlanRoute: () -> Unit,
    onOpenSetup: () -> Unit,
    viewModel: SimulationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                SimulationEvent.NavigateToPlanRoute -> onPlanRoute()
                SimulationEvent.NavigateToSetup -> onOpenSetup()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(SimulationAction.OnScreenResumed)
    }

    SimulationScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun SimulationScreen(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
) {
    var previousSessionWasNull by remember { mutableStateOf(true) }
    var previousRealWasNull by remember { mutableStateOf(true) }
    var cameraTarget by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    LaunchedEffect(state.mockedSession) {
        val session = state.mockedSession
        if (session != null && previousSessionWasNull) {
            cameraTarget = session.latitude to session.longitude
        }
        previousSessionWasNull = session == null
    }
    LaunchedEffect(state.realLocation, state.mockedSession == null) {
        if (state.mockedSession == null) {
            val real = state.realLocation
            if (real != null && previousRealWasNull) {
                cameraTarget = real.latitude to real.longitude
            }
            previousRealWasNull = real == null
        }
    }

    val markerBackground = MaterialTheme.colorScheme.primary.toArgb()
    val markerText = MaterialTheme.colorScheme.onPrimary.toArgb()
    val routeProgressCalculator = remember { RouteProgressCalculator() }
    val loadedRoute = state.loadedRoute
    val routeProgress =
        loadedRoute?.let {
            val distanceTraveled = state.mockedSession?.takeIf { session -> session.mode == SimulationMode.ROUTE }?.distanceTraveledMeters ?: 0.0
            routeProgressCalculator.interpolate(it, distanceTraveled)
        }
    val traveledPolylinePoints =
        if (loadedRoute != null && routeProgress != null) {
            loadedRoute.geometry.subList(0, (routeProgress.segmentIndex + 1).coerceAtMost(loadedRoute.geometry.size)) +
                (routeProgress.latitude to routeProgress.longitude)
        } else {
            emptyList()
        }

    val markers =
        buildList {
            loadedRoute?.points?.forEachIndexed { index, point ->
                add(
                    RouteForgeMapMarker(
                        id = "waypoint-$index",
                        latitude = point.latitude,
                        longitude = point.longitude,
                        icon = RouteForgeMapMarkerIcon.Numbered(index + 1, markerBackground, markerText),
                    ),
                )
            }
            state.mockedSession?.let {
                add(
                    RouteForgeMapMarker(
                        id = "mocked",
                        latitude = it.latitude,
                        longitude = it.longitude,
                        icon = RouteForgeMapMarkerIcon.Arrow(AndroidColor.RED),
                        rotationDegrees = it.bearingDegrees,
                    ),
                )
            }
            if (state.mockedSession == null) {
                state.realLocation?.let {
                    add(
                        RouteForgeMapMarker(
                            id = "real",
                            latitude = it.latitude,
                            longitude = it.longitude,
                            icon = RouteForgeMapMarkerIcon.Dot(AndroidColor.BLUE),
                        ),
                    )
                }
            }
        }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            RouteForgeMap(
                markers = markers,
                polylinePoints = state.loadedRoute?.geometry.orEmpty(),
                traveledPolylinePoints = traveledPolylinePoints,
                cameraTarget = cameraTarget,
                onMapTap = { latitude, longitude -> onAction(SimulationAction.OnMapTap(latitude, longitude)) },
                modifier = Modifier.fillMaxSize(),
            )

            CoordinatePill(
                mockedSession = state.mockedSession,
                isSearchingRealLocation = state.isSearchingRealLocation,
                onCancelMockClick = { onAction(SimulationAction.OnCancelMockClick) },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = ScreenContentPadding),
            )

            if (state.isBlockedByAuthorization) {
                TopBanner(
                    contentPadding = ScreenContentPadding,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = BannerTopOffset, start = ScreenContentPadding, end = ScreenContentPadding),
                ) {
                    Text(stringResource(R.string.simulation_error_not_authorized))
                    Button(onClick = { onAction(SimulationAction.OnOpenSetupClick) }) {
                        Text(stringResource(R.string.simulation_go_to_setup_button))
                    }
                }
            } else {
                state.errorType?.let { errorType ->
                    TopBanner(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = BannerTopOffset, start = ScreenContentPadding, end = ScreenContentPadding),
                    ) {
                        Text(text = errorType.toMessage())
                    }
                }
            }

            var isJoystickSpeedDialogOpen by remember { mutableStateOf(false) }
            var isPlaybackSpeedDialogOpen by remember { mutableStateOf(false) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ControlsRowSpacing),
                modifier = Modifier.align(Alignment.BottomStart).padding(ScreenContentPadding),
            ) {
                if (state.loadedRoute != null) {
                    // A route and the joystick are mutually exclusive (engaging one clears the
                    // other), so only the controls relevant to what's actually loaded are shown.
                    SpeedSelectorFab(
                        onClick = { isPlaybackSpeedDialogOpen = true },
                        contentDescription = stringResource(R.string.simulation_playback_speed_button),
                    )
                } else {
                    if (state.isJoystickVisible) {
                        SpeedSelectorFab(
                            onClick = { isJoystickSpeedDialogOpen = true },
                            contentDescription = stringResource(R.string.simulation_joystick_speed_button),
                        )
                    }
                    FloatingActionButton(onClick = { onAction(SimulationAction.OnToggleJoystick) }) {
                        Icon(Icons.Filled.SportsEsports, contentDescription = stringResource(R.string.simulation_joystick_label))
                    }
                }
            }
            if (isPlaybackSpeedDialogOpen) {
                SpeedSelectorDialog(
                    title = stringResource(R.string.simulation_playback_speed_dialog_title),
                    speedLabel = stringResource(R.string.simulation_speed_value_label, state.playbackSpeedKmh.roundToInt()),
                    confirmButtonLabel = stringResource(R.string.simulation_speed_dialog_close),
                    speedKmh = state.playbackSpeedKmh,
                    onSpeedChange = { kmh -> onAction(SimulationAction.OnPlaybackSpeedChange(kmh)) },
                    onDismiss = { isPlaybackSpeedDialogOpen = false },
                    speedRangeKmh = SpeedSelectorRangeKmh,
                )
            }

            if (state.isJoystickVisible) {
                Joystick(
                    onDrag = { bearing -> onAction(SimulationAction.OnJoystickDrag(bearing)) },
                    onReleased = { onAction(SimulationAction.OnJoystickReleased) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = JoystickBottomPadding),
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

            if (state.loadedRoute != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ControlsRowSpacing),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(ScreenContentPadding),
                ) {
                    FloatingActionButton(onClick = { onAction(SimulationAction.OnStopSimulation) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.simulation_cancel_route_button))
                    }
                    PlaybackButton(state = state, onAction = onAction)
                }
            } else {
                FloatingActionButton(
                    onClick = { onAction(SimulationAction.OnPlanRouteClick) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(ScreenContentPadding),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.simulation_plan_route_button))
                }
            }
        }
    }

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

    if (state.isJoystickInterruptPending) {
        JoystickInterruptSheet(
            onConfirm = { onAction(SimulationAction.OnConfirmJoystickInterrupt) },
            onDismiss = { onAction(SimulationAction.OnDismissJoystickInterrupt) },
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
