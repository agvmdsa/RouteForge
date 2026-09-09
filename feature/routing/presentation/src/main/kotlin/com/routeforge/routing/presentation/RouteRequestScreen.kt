package com.routeforge.routing.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.routing.domain.model.Route
import org.koin.androidx.compose.koinViewModel

@Composable
fun RouteRequestRoot(
    onRouteComputed: (Route) -> Unit,
    onOpenRegionCatalog: () -> Unit,
    viewModel: RouteRequestViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RouteRequestEvent.RouteComputed -> onRouteComputed(event.route)
                RouteRequestEvent.NavigateToRegionCatalog -> onOpenRegionCatalog()
            }
        }
    }

    RouteRequestScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun RouteRequestScreen(
    state: RouteRequestState,
    onAction: (RouteRequestAction) -> Unit,
) {
    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(text = "Start point", style = MaterialTheme.typography.titleMedium)
            }
            item {
                CoordinateRow(
                    latitude = state.startLatitudeInput,
                    longitude = state.startLongitudeInput,
                    onLatitudeChange = { onAction(RouteRequestAction.OnStartLatitudeChange(it)) },
                    onLongitudeChange = { onAction(RouteRequestAction.OnStartLongitudeChange(it)) },
                )
            }

            item {
                Text(text = "Waypoints", style = MaterialTheme.typography.titleMedium)
            }
            items(state.waypoints.size) { index ->
                val waypoint = state.waypoints[index]
                Column {
                    CoordinateRow(
                        latitude = waypoint.latitudeInput,
                        longitude = waypoint.longitudeInput,
                        onLatitudeChange = { onAction(RouteRequestAction.OnWaypointLatitudeChange(index, it)) },
                        onLongitudeChange = { onAction(RouteRequestAction.OnWaypointLongitudeChange(index, it)) },
                    )
                    TextButton(onClick = { onAction(RouteRequestAction.OnRemoveWaypoint(index)) }) {
                        Text("Remove waypoint")
                    }
                }
            }
            item {
                TextButton(onClick = { onAction(RouteRequestAction.OnAddWaypoint) }) {
                    Text("Add waypoint")
                }
            }

            item {
                Text(text = "End point", style = MaterialTheme.typography.titleMedium)
            }
            item {
                CoordinateRow(
                    latitude = state.endLatitudeInput,
                    longitude = state.endLongitudeInput,
                    onLatitudeChange = { onAction(RouteRequestAction.OnEndLatitudeChange(it)) },
                    onLongitudeChange = { onAction(RouteRequestAction.OnEndLongitudeChange(it)) },
                )
            }

            item {
                Button(
                    onClick = { onAction(RouteRequestAction.OnRequestRoute) },
                    enabled = !state.isComputing,
                ) {
                    Text("Get Route")
                }
            }

            if (state.isComputing) {
                item { CircularProgressIndicator() }
            }

            state.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            state.route?.let { route ->
                item {
                    Text(
                        text = "Route found: ${route.distanceMeters.toInt()} m, ${route.geometry.size} points",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                TextButton(onClick = { onAction(RouteRequestAction.OnOpenRegionCatalog) }) {
                    Text("Manage offline regions")
                }
            }
        }
    }
}

@Composable
private fun CoordinateRow(
    latitude: String,
    longitude: String,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = latitude,
            onValueChange = onLatitudeChange,
            label = { Text("Latitude") },
            modifier = Modifier.fillMaxWidth(0.5f),
        )
        OutlinedTextField(
            value = longitude,
            onValueChange = onLongitudeChange,
            label = { Text("Longitude") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun RouteRequestScreenPreview() {
    RouteForgeTheme {
        RouteRequestScreen(state = RouteRequestState(), onAction = {})
    }
}

@Preview
@Composable
private fun RouteRequestScreenErrorPreview() {
    RouteForgeTheme {
        RouteRequestScreen(
            state = RouteRequestState(errorMessage = "That point is too far from any known road to route from it."),
            onAction = {},
        )
    }
}
