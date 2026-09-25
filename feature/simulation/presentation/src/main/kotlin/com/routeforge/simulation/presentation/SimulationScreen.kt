package com.routeforge.simulation.presentation

import android.Manifest
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.designsystem.map.RouteForgeMap
import com.routeforge.designsystem.map.RouteForgeMapMarker
import com.routeforge.designsystem.map.RouteForgeMapMarkerIcon
import com.routeforge.designsystem.speed.SpeedSelectorDialog
import com.routeforge.designsystem.speed.SpeedSelectorFab
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SimulationStatus
import org.koin.androidx.compose.koinViewModel
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val ScreenContentPadding = 16.dp
private val ControlsRowSpacing = 8.dp
private val JoystickDiameter = 120.dp
private val KnobDiameter = 36.dp
private val BigButtonDiameter = 72.dp
private val JoystickBottomPadding = 24.dp
private val PillShape = RoundedCornerShape(28.dp)
private val BannerShape = RoundedCornerShape(12.dp)
private val BannerTopOffset = 72.dp
private val SurfaceElevation = 4.dp
private val PillHorizontalPadding = 16.dp
private val PillVerticalPadding = 8.dp
private val BannerContentPadding = 12.dp
private const val MIN_SPEED_KMH = 0f
private const val MAX_SPEED_KMH = 150f
private val SpeedSelectorRangeKmh = MIN_SPEED_KMH..MAX_SPEED_KMH
private const val MPS_TO_KMH_MULTIPLIER = 3.6f

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

    val markers =
        buildList {
            state.mockedSession?.let {
                add(
                    RouteForgeMapMarker(
                        id = "mocked",
                        latitude = it.latitude,
                        longitude = it.longitude,
                        icon = RouteForgeMapMarkerIcon.Dot(AndroidColor.RED),
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
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = BannerShape,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = BannerTopOffset, start = ScreenContentPadding, end = ScreenContentPadding),
                ) {
                    Column(modifier = Modifier.padding(ScreenContentPadding)) {
                        Text(stringResource(R.string.simulation_error_not_authorized))
                        Button(onClick = { onAction(SimulationAction.OnOpenSetupClick) }) {
                            Text(stringResource(R.string.simulation_go_to_setup_button))
                        }
                    }
                }
            } else {
                state.errorType?.let { errorType ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = BannerShape,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = BannerTopOffset, start = ScreenContentPadding, end = ScreenContentPadding),
                    ) {
                        Text(text = errorType.toMessage(), modifier = Modifier.padding(BannerContentPadding))
                    }
                }
            }

            var isJoystickSpeedDialogOpen by remember { mutableStateOf(false) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ControlsRowSpacing),
                modifier = Modifier.align(Alignment.BottomStart).padding(ScreenContentPadding),
            ) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = ScreenContentPadding),
                ) {
                    if (state.mockedSession?.mode == SimulationMode.ROUTE) {
                        FloatingActionButton(onClick = { onAction(SimulationAction.OnStopSimulation) }) {
                            Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.simulation_stop_button))
                        }
                    }
                    PlaybackButton(state = state, onAction = onAction)
                    var isTuneSheetOpen by remember { mutableStateOf(false) }
                    FloatingActionButton(onClick = { isTuneSheetOpen = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.simulation_tune_button))
                    }
                    if (isTuneSheetOpen) {
                        val sheetState = rememberModalBottomSheetState()
                        ModalBottomSheet(onDismissRequest = { isTuneSheetOpen = false }, sheetState = sheetState) {
                            PlaybackTuningSheetContent(state = state, onAction = onAction)
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { onAction(SimulationAction.OnPlanRouteClick) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(ScreenContentPadding),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.simulation_plan_route_button))
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
}

@Composable
private fun CoordinatePill(
    mockedSession: SimulationSession?,
    isSearchingRealLocation: Boolean,
    onCancelMockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val text =
        when {
            mockedSession != null -> "%.5f, %.5f".format(mockedSession.latitude, mockedSession.longitude)
            isSearchingRealLocation -> stringResource(R.string.simulation_status_searching_real_location)
            else -> stringResource(R.string.simulation_status_no_simulation)
        }
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = SurfaceElevation,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = PillHorizontalPadding, vertical = PillVerticalPadding),
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
            if (mockedSession != null) {
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(text)) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.simulation_copy_coordinates))
                }
            }
            if (mockedSession?.mode == SimulationMode.STATIONARY) {
                IconButton(onClick = onCancelMockClick) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.simulation_cancel_mock_button))
                }
            }
        }
    }
}

@Composable
private fun PlaybackButton(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
) {
    val session = state.mockedSession
    val (icon, description, action) =
        if (session?.mode == SimulationMode.ROUTE && session.status == SimulationStatus.RUNNING) {
            Triple(Icons.Filled.Pause, stringResource(R.string.simulation_pause_button), SimulationAction.OnPauseSimulation)
        } else if (session?.mode == SimulationMode.ROUTE && session.status == SimulationStatus.PAUSED) {
            Triple(Icons.Filled.PlayArrow, stringResource(R.string.simulation_resume_button), SimulationAction.OnResumeSimulation)
        } else {
            Triple(Icons.Filled.PlayArrow, stringResource(R.string.simulation_start_button), SimulationAction.OnStartRouteSimulation)
        }
    FloatingActionButton(
        onClick = { onAction(action) },
        modifier = Modifier.size(BigButtonDiameter),
    ) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun PlaybackTuningSheetContent(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
) {
    Column(modifier = Modifier.padding(ScreenContentPadding)) {
        Text(
            text =
                stringResource(
                    R.string.simulation_effective_speed_label,
                    (state.mockedSession?.speedMetersPerSecond ?: 0f) * MPS_TO_KMH_MULTIPLIER,
                ),
            style = MaterialTheme.typography.titleMedium,
        )

        var isPlaybackSpeedDialogOpen by remember { mutableStateOf(false) }
        SpeedSelectorFab(
            onClick = { isPlaybackSpeedDialogOpen = true },
            contentDescription = stringResource(R.string.simulation_playback_speed_button),
        )
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

        if (state.mockedSession?.mode != SimulationMode.ROUTE) {
            Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
                ExecutionModeSelection.entries.forEach { selection ->
                    TextButton(onClick = { onAction(SimulationAction.OnExecutionModeSelected(selection)) }) {
                        Text(selection.toLabel())
                    }
                }
            }
            if (state.executionModeSelection == ExecutionModeSelection.TIMES) {
                OutlinedTextField(
                    value = state.executionTimesInput,
                    onValueChange = { onAction(SimulationAction.OnExecutionTimesInputChange(it)) },
                    label = { Text(stringResource(R.string.simulation_execution_times_input_label)) },
                )
            }
        }
    }
}

@Composable
private fun TeleportConfirmationSheet(
    latitude: Double,
    longitude: Double,
    isBlockedOffline: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(ScreenContentPadding)) {
            Text(stringResource(R.string.simulation_teleport_confirm_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.simulation_teleport_confirm_message, latitude, longitude))
            if (isBlockedOffline) {
                Text(
                    text = stringResource(R.string.simulation_teleport_blocked_offline_message),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
                Button(onClick = onConfirm, enabled = !isBlockedOffline) {
                    Text(stringResource(R.string.simulation_teleport_confirm_button))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.simulation_teleport_cancel_button)) }
            }
        }
    }
}

@Composable
private fun CancelMockConfirmationSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(ScreenContentPadding)) {
            Text(stringResource(R.string.simulation_cancel_mock_confirm_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.simulation_cancel_mock_confirm_message))
            Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
                Button(onClick = onConfirm) { Text(stringResource(R.string.simulation_cancel_mock_confirm_button)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.simulation_cancel_mock_dismiss_button)) }
            }
        }
    }
}

@Composable
private fun JoystickInterruptSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(ScreenContentPadding)) {
            Text(stringResource(R.string.simulation_joystick_interrupt_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.simulation_joystick_interrupt_message))
            Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
                Button(onClick = onConfirm) { Text(stringResource(R.string.simulation_joystick_interrupt_confirm)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.simulation_joystick_interrupt_cancel)) }
            }
        }
    }
}

/** FR-022/FR-023: drag anywhere on this pad to move; bearing is measured clockwise from north.
 *  The inner knob visually tracks the drag so the current direction is always obvious. */
@Composable
private fun Joystick(
    onDrag: (Float) -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var totalDrag by remember { mutableStateOf(Offset.Zero) }
    val maxKnobOffsetPx = with(LocalDensity.current) { ((JoystickDiameter - KnobDiameter) / 2).toPx() }
    val knobOffset =
        run {
            val distance = sqrt(totalDrag.x * totalDrag.x + totalDrag.y * totalDrag.y)
            if (distance <= maxKnobOffsetPx || distance == 0f) totalDrag else totalDrag * (maxKnobOffsetPx / distance)
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(JoystickDiameter)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            totalDrag = Offset.Zero
                            onReleased()
                        },
                        onDragCancel = {
                            totalDrag = Offset.Zero
                            onReleased()
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                        val bearing =
                            ((Math.toDegrees(atan2(totalDrag.x.toDouble(), -totalDrag.y.toDouble())) + 360.0) % 360.0).toFloat()
                        onDrag(bearing)
                    }
                },
    ) {
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                    .size(KnobDiameter)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
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

@Composable
private fun ExecutionModeSelection.toLabel(): String =
    when (this) {
        ExecutionModeSelection.ONCE -> stringResource(R.string.simulation_execution_once)
        ExecutionModeSelection.TIMES -> stringResource(R.string.simulation_execution_times)
        ExecutionModeSelection.LOOP -> stringResource(R.string.simulation_execution_loop)
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
