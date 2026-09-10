package com.routeforge.simulation.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.coredomain.model.Route
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SimulationStatus
import org.koin.androidx.compose.koinViewModel

private const val BASE_PIXELS_PER_DEGREE = 4_000f

@Composable
fun SimulationRoot(
    onPlanRoute: () -> Unit,
    onOpenSetup: () -> Unit,
    viewModel: SimulationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            SimulationCanvas(
                route = state.loadedRoute,
                session = state.session,
                onTap = { latitude, longitude -> onAction(SimulationAction.OnMapTap(latitude, longitude)) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            SessionStatusText(session = state.session)

            if (state.isBlockedByAuthorization) {
                BlockedByAuthorizationContent(
                    message = state.errorMessage.orEmpty(),
                    onOpenSetup = { onAction(SimulationAction.OnOpenSetupClick) },
                )
            } else {
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            RouteSimulationControls(state = state, onAction = onAction)

            TextButton(onClick = { onAction(SimulationAction.OnPlanRouteClick) }) {
                Text("Plan a route")
            }
        }
    }

    val pendingLatitude = state.pendingTeleportLatitude
    val pendingLongitude = state.pendingTeleportLongitude
    if (pendingLatitude != null && pendingLongitude != null) {
        TeleportConfirmationDialog(
            latitude = pendingLatitude,
            longitude = pendingLongitude,
            onConfirm = { onAction(SimulationAction.OnConfirmTeleport) },
            onDismiss = { onAction(SimulationAction.OnCancelTeleport) },
        )
    }
}

@Composable
private fun SimulationCanvas(
    route: Route?,
    session: SimulationSession?,
    onTap: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    val currentOnTap by rememberUpdatedState(onTap)

    Canvas(
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        offset += pan
                        scale = (scale * zoom).coerceIn(0.1f, 20f)
                    }
                }.pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val pixelsPerDegree = BASE_PIXELS_PER_DEGREE * scale
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val longitude = ((tapOffset.x - centerX - offset.x) / pixelsPerDegree).toDouble()
                        val latitude = (-(tapOffset.y - centerY - offset.y) / pixelsPerDegree).toDouble()
                        currentOnTap(latitude, longitude)
                    }
                },
    ) {
        val pixelsPerDegree = BASE_PIXELS_PER_DEGREE * scale
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        fun project(
            latitude: Double,
            longitude: Double,
        ): Offset =
            Offset(
                x = centerX + offset.x + (longitude * pixelsPerDegree).toFloat(),
                y = centerY + offset.y - (latitude * pixelsPerDegree).toFloat(),
            )

        route?.geometry?.let { geometry ->
            for (index in 0 until geometry.size - 1) {
                val (startLat, startLon) = geometry[index]
                val (endLat, endLon) = geometry[index + 1]
                drawLine(
                    color = Color.Blue,
                    start = project(startLat, startLon),
                    end = project(endLat, endLon),
                    strokeWidth = 4f,
                )
            }
        }

        session?.let {
            drawCircle(color = Color.Red, radius = 12f, center = project(it.latitude, it.longitude))
        }
    }
}

@Composable
private fun TeleportConfirmationDialog(
    latitude: Double,
    longitude: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teleport to this location?") },
        text = { Text("Latitude: $latitude\nLongitude: $longitude") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SessionStatusText(session: SimulationSession?) {
    val text =
        when {
            session == null -> "No simulation active"
            session.mode == SimulationMode.STATIONARY -> "Teleported: reporting a fixed location"
            session.status == SimulationStatus.PAUSED -> "Route simulation paused"
            session.status == SimulationStatus.COMPLETED -> "Route simulation complete"
            else -> "Simulating movement along the route"
        }
    Text(text = text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun BlockedByAuthorizationContent(
    message: String,
    onOpenSetup: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onOpenSetup) {
            Text("Go to setup")
        }
    }
}

@Composable
private fun RouteSimulationControls(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
) {
    val route = state.loadedRoute ?: return
    val session = state.session

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Loaded route: ${route.distanceMeters.toInt()} m", style = MaterialTheme.typography.bodyMedium)

        if (session == null || session.mode != SimulationMode.ROUTE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.speedInput,
                    onValueChange = { onAction(SimulationAction.OnSpeedInputChange(it)) },
                    label = { Text("Speed (m/s)") },
                )
                Button(onClick = { onAction(SimulationAction.OnStartRouteSimulation) }) {
                    Text("Start simulation")
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (session.status) {
                    SimulationStatus.RUNNING ->
                        Button(onClick = { onAction(SimulationAction.OnPauseSimulation) }) {
                            Text("Pause")
                        }
                    SimulationStatus.PAUSED ->
                        Button(onClick = { onAction(SimulationAction.OnResumeSimulation) }) {
                            Text("Resume")
                        }
                    SimulationStatus.COMPLETED -> Unit
                }
                Button(onClick = { onAction(SimulationAction.OnStopSimulation) }) {
                    Text("Stop")
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
                    errorMessage = "RouteForge isn't set up as the device's mock location provider yet.",
                ),
            onAction = {},
        )
    }
}
