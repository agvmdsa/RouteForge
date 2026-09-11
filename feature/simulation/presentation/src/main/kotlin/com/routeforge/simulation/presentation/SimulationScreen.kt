package com.routeforge.simulation.presentation

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.coredomain.model.Route
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.simulation.domain.model.RealLocation
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SimulationStatus
import org.koin.androidx.compose.koinViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private const val OSMDROID_PREFS_NAME = "osmdroid_config"
private const val DEFAULT_MAP_ZOOM = 15.0
private const val FALLBACK_WORLD_MAP_ZOOM = 3.0
private const val ROUTE_LINE_WIDTH_PX = 6f
private const val MOCKED_LOCATION_DOT_COLOR = android.graphics.Color.RED
private const val REAL_LOCATION_DOT_COLOR = android.graphics.Color.BLUE
private val LocationDotSize = 20.dp

private val ScreenContentPadding = 16.dp
private val ControlsRowSpacing = 8.dp

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
    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SimulationMap(
                route = state.loadedRoute,
                mockedSession = state.mockedSession,
                realLocation = state.realLocation,
                onTap = { latitude, longitude -> onAction(SimulationAction.OnMapTap(latitude, longitude)) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            LocationStatusText(
                mockedSession = state.mockedSession,
                isSearchingRealLocation = state.isSearchingRealLocation,
            )

            if (state.isBlockedByAuthorization) {
                BlockedByAuthorizationContent(
                    message = stringResource(R.string.simulation_error_not_authorized),
                    onOpenSetup = { onAction(SimulationAction.OnOpenSetupClick) },
                )
            } else {
                state.errorType?.let { errorType ->
                    Text(
                        text = errorType.toMessage(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = ScreenContentPadding),
                    )
                }
            }

            if (state.mockedSession?.mode == SimulationMode.STATIONARY) {
                TextButton(onClick = { onAction(SimulationAction.OnCancelMockClick) }) {
                    Text(stringResource(R.string.simulation_cancel_mock_button))
                }
            }

            RouteSimulationControls(state = state, onAction = onAction)

            TextButton(onClick = { onAction(SimulationAction.OnPlanRouteClick) }) {
                Text(stringResource(R.string.simulation_plan_route_button))
            }
        }
    }

    val pendingLatitude = state.pendingTeleportLatitude
    val pendingLongitude = state.pendingTeleportLongitude
    if (pendingLatitude != null && pendingLongitude != null) {
        TeleportConfirmationDialog(
            latitude = pendingLatitude,
            longitude = pendingLongitude,
            isBlockedOffline = state.isPendingTeleportBlockedOffline,
            onConfirm = { onAction(SimulationAction.OnConfirmTeleport) },
            onDismiss = { onAction(SimulationAction.OnCancelTeleport) },
        )
    }

    if (state.isPendingCancelMock) {
        CancelMockConfirmationDialog(
            onConfirm = { onAction(SimulationAction.OnConfirmCancelMock) },
            onDismiss = { onAction(SimulationAction.OnDismissCancelMock) },
        )
    }
}

private data class SimulationMapComponents(
    val mapView: MapView,
    val routeOverlay: Polyline,
    val mockedLocationMarker: Marker,
    val realLocationMarker: Marker,
)

private fun createDotDrawable(
    context: Context,
    colorInt: Int,
    sizePx: Int,
): BitmapDrawable {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.FILL
        }
    val radius = sizePx / 2f
    canvas.drawCircle(radius, radius, radius, paint)
    return BitmapDrawable(context.resources, bitmap)
}

private fun buildSimulationMapComponents(
    context: Context,
    density: Density,
    onTap: (Double, Double) -> Unit,
): SimulationMapComponents {
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences(OSMDROID_PREFS_NAME, Context.MODE_PRIVATE))
        userAgentValue = context.packageName
    }

    val mapView =
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(FALLBACK_WORLD_MAP_ZOOM)
        }
    val routeOverlay = Polyline().apply { outlinePaint.strokeWidth = ROUTE_LINE_WIDTH_PX }
    val locationDotSizePx = with(density) { LocationDotSize.roundToPx() }
    val mockedLocationMarker =
        Marker(mapView).apply {
            icon = createDotDrawable(context, MOCKED_LOCATION_DOT_COLOR, locationDotSizePx)
        }
    val realLocationMarker =
        Marker(mapView).apply {
            icon = createDotDrawable(context, REAL_LOCATION_DOT_COLOR, locationDotSizePx)
        }
    val tapOverlay =
        MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                    onTap(point.latitude, point.longitude)
                    return true
                }

                override fun longPressHelper(point: GeoPoint): Boolean = false
            },
        )

    mapView.overlays.add(tapOverlay)
    mapView.overlays.add(routeOverlay)

    return SimulationMapComponents(mapView, routeOverlay, mockedLocationMarker, realLocationMarker)
}

@Composable
private fun SimulationMap(
    route: Route?,
    mockedSession: SimulationSession?,
    realLocation: RealLocation?,
    onTap: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnTap by rememberUpdatedState(onTap)

    val components =
        remember {
            buildSimulationMapComponents(
                context = context,
                density = density,
                onTap = { latitude, longitude -> currentOnTap(latitude, longitude) },
            )
        }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> components.mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> components.mapView.onPause()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            components.mapView.onDetach()
        }
    }

    LaunchedEffect(route) {
        val points = route?.geometry?.map { (latitude, longitude) -> GeoPoint(latitude, longitude) }.orEmpty()
        components.routeOverlay.setPoints(points)
        components.mapView.invalidate()
    }

    LaunchedEffect(mockedSession) {
        val marker = components.mockedLocationMarker
        val wasVisible = components.mapView.overlays.contains(marker)
        if (mockedSession == null) {
            components.mapView.overlays.remove(marker)
        } else {
            marker.position = GeoPoint(mockedSession.latitude, mockedSession.longitude)
            if (!wasVisible) {
                components.mapView.overlays.add(marker)
                components.mapView.controller.setZoom(DEFAULT_MAP_ZOOM)
                components.mapView.controller.setCenter(marker.position)
            }
        }
        components.mapView.invalidate()
    }

    LaunchedEffect(realLocation, mockedSession) {
        val marker = components.realLocationMarker
        val wasVisible = components.mapView.overlays.contains(marker)
        if (mockedSession != null || realLocation == null) {
            components.mapView.overlays.remove(marker)
        } else {
            marker.position = GeoPoint(realLocation.latitude, realLocation.longitude)
            if (!wasVisible) {
                components.mapView.overlays.add(marker)
                components.mapView.controller.setZoom(DEFAULT_MAP_ZOOM)
                components.mapView.controller.setCenter(marker.position)
            }
        }
        components.mapView.invalidate()
    }

    AndroidView(factory = { components.mapView }, modifier = modifier)
}

@Composable
private fun TeleportConfirmationDialog(
    latitude: Double,
    longitude: Double,
    isBlockedOffline: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.simulation_teleport_confirm_title)) },
        text = {
            Column {
                Text(stringResource(R.string.simulation_teleport_confirm_message, latitude, longitude))
                if (isBlockedOffline) {
                    Text(
                        text = stringResource(R.string.simulation_teleport_blocked_offline_message),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isBlockedOffline) {
                Text(stringResource(R.string.simulation_teleport_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.simulation_teleport_cancel_button)) }
        },
    )
}

@Composable
private fun CancelMockConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.simulation_cancel_mock_confirm_title)) },
        text = { Text(stringResource(R.string.simulation_cancel_mock_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.simulation_cancel_mock_confirm_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.simulation_cancel_mock_dismiss_button)) }
        },
    )
}

@Composable
private fun SimulationErrorType.toMessage(): String =
    when (this) {
        SimulationErrorType.NOT_AUTHORIZED -> stringResource(R.string.simulation_error_not_authorized)
        SimulationErrorType.INVALID_SPEED -> stringResource(R.string.simulation_error_invalid_speed)
    }

@Composable
private fun LocationStatusText(
    mockedSession: SimulationSession?,
    isSearchingRealLocation: Boolean,
) {
    val text =
        when {
            mockedSession == null && isSearchingRealLocation ->
                stringResource(R.string.simulation_status_searching_real_location)
            mockedSession == null -> stringResource(R.string.simulation_status_no_simulation)
            mockedSession.mode == SimulationMode.STATIONARY -> stringResource(R.string.simulation_status_teleported)
            mockedSession.status == SimulationStatus.PAUSED -> stringResource(R.string.simulation_status_route_paused)
            mockedSession.status == SimulationStatus.COMPLETED ->
                stringResource(R.string.simulation_status_route_complete)
            else -> stringResource(R.string.simulation_status_route_running)
        }
    Text(text = text, modifier = Modifier.padding(ScreenContentPadding), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun BlockedByAuthorizationContent(
    message: String,
    onOpenSetup: () -> Unit,
) {
    Column(modifier = Modifier.padding(ScreenContentPadding)) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onOpenSetup) {
            Text(stringResource(R.string.simulation_go_to_setup_button))
        }
    }
}

@Composable
private fun RouteSimulationControls(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
) {
    val route = state.loadedRoute ?: return
    val mockedSession = state.mockedSession

    Column(modifier = Modifier.padding(ScreenContentPadding)) {
        Text(
            text = stringResource(R.string.simulation_loaded_route_label, route.distanceMeters.toInt()),
            style = MaterialTheme.typography.bodyMedium,
        )

        if (mockedSession == null || mockedSession.mode != SimulationMode.ROUTE) {
            Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
                OutlinedTextField(
                    value = state.speedInput,
                    onValueChange = { onAction(SimulationAction.OnSpeedInputChange(it)) },
                    label = { Text(stringResource(R.string.simulation_speed_input_label)) },
                )
                Button(onClick = { onAction(SimulationAction.OnStartRouteSimulation) }) {
                    Text(stringResource(R.string.simulation_start_button))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
                when (mockedSession.status) {
                    SimulationStatus.RUNNING ->
                        Button(onClick = { onAction(SimulationAction.OnPauseSimulation) }) {
                            Text(stringResource(R.string.simulation_pause_button))
                        }
                    SimulationStatus.PAUSED ->
                        Button(onClick = { onAction(SimulationAction.OnResumeSimulation) }) {
                            Text(stringResource(R.string.simulation_resume_button))
                        }
                    SimulationStatus.COMPLETED -> Unit
                }
                Button(onClick = { onAction(SimulationAction.OnStopSimulation) }) {
                    Text(stringResource(R.string.simulation_stop_button))
                }
            }
        }
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
