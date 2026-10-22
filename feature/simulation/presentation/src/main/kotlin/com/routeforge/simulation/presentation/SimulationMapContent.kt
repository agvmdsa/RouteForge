package com.routeforge.simulation.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import com.routeforge.coredomain.model.LatLng
import com.routeforge.designsystem.map.RouteForgeMapMarker
import com.routeforge.designsystem.map.RouteForgeMapMarkerIcon
import com.routeforge.simulation.domain.RouteProgressCalculator
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.presentation.waypointedit.WaypointEditAction
import com.routeforge.simulation.presentation.waypointedit.WaypointEditState
import android.graphics.Color as AndroidColor

/** Re-centers the map once each time a mocked session starts, or — only while no mocked session is
 *  active — once each time a real-location fix first becomes available. [previousSessionWasNull]/
 *  [previousRealWasNull] track the transition so later updates to an already-active source don't
 *  keep yanking the camera back. */
@Composable
internal fun rememberSimulationCameraTarget(state: SimulationState): LatLng? {
    var previousSessionWasNull by remember { mutableStateOf(true) }
    var previousRealWasNull by remember { mutableStateOf(true) }
    var cameraTarget by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(state.mockedSession) {
        val session = state.mockedSession
        if (session != null && previousSessionWasNull) {
            cameraTarget = LatLng(session.latitude, session.longitude)
        }
        previousSessionWasNull = session == null
    }
    LaunchedEffect(state.realLocation, state.mockedSession == null) {
        if (state.mockedSession == null) {
            val real = state.realLocation
            if (real != null && previousRealWasNull) {
                cameraTarget = LatLng(real.latitude, real.longitude)
            }
            previousRealWasNull = real == null
        }
    }
    return cameraTarget
}

internal data class SimulationMapContent(
    val markers: List<RouteForgeMapMarker>,
    val polylinePoints: List<LatLng>,
    val traveledPolylinePoints: List<LatLng>,
    val sessionDistanceTraveledMeters: Double?,
)

/** Builds the map's waypoint/mocked-position/real-position markers and both polylines (the full
 *  route, and the traveled-so-far portion), substituting a marker's live drag position into the
 *  main polyline while it's being dragged so the line visibly follows the gesture. */
@Composable
internal fun rememberSimulationMapContent(
    state: SimulationState,
    editState: WaypointEditState,
    onEditAction: (WaypointEditAction) -> Unit,
): SimulationMapContent {
    val markerBackground = MaterialTheme.colorScheme.primary.toArgb()
    val markerMutedBackground = MaterialTheme.colorScheme.outline.toArgb()
    val markerText = MaterialTheme.colorScheme.onPrimary.toArgb()
    val routeProgressCalculator = remember { RouteProgressCalculator() }
    val loadedRoute = state.loadedRoute
    val sessionDistanceTraveledMeters =
        state.mockedSession?.takeIf { session -> session.mode == SimulationMode.ROUTE }?.distanceTraveledMeters
    val routeProgress = loadedRoute?.let { routeProgressCalculator.interpolate(it, sessionDistanceTraveledMeters ?: 0.0) }
    val traveledPolylinePoints =
        if (loadedRoute != null && routeProgress != null) {
            loadedRoute.geometry.subList(0, (routeProgress.segmentIndex + 1).coerceAtMost(loadedRoute.geometry.size)) +
                LatLng(routeProgress.latitude, routeProgress.longitude)
        } else {
            emptyList()
        }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var draggingPosition by remember { mutableStateOf<LatLng?>(null) }
    val markers =
        buildList {
            loadedRoute?.points?.forEachIndexed { index, point ->
                val isReadOnly = editState.isOpen && index < editState.firstEditableIndex
                add(
                    RouteForgeMapMarker(
                        id = "waypoint-$index",
                        latitude = point.latitude,
                        longitude = point.longitude,
                        icon =
                            RouteForgeMapMarkerIcon.Numbered(
                                index + 1,
                                if (isReadOnly) markerMutedBackground else markerBackground,
                                markerText,
                            ),
                        draggable = editState.isOpen && !isReadOnly,
                        onDrag = { latitude, longitude ->
                            draggingIndex = index
                            draggingPosition = LatLng(latitude, longitude)
                        },
                        onDragEnd = { latitude, longitude ->
                            draggingIndex = null
                            draggingPosition = null
                            onEditAction(WaypointEditAction.OnMarkerDragged(index, latitude, longitude))
                        },
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

    val liveDraggingPosition = draggingPosition
    val mainPolylinePoints =
        if (draggingIndex != null && liveDraggingPosition != null && loadedRoute != null) {
            loadedRoute.points.mapIndexed { i, p -> if (i == draggingIndex) liveDraggingPosition else LatLng(p.latitude, p.longitude) }
        } else {
            state.loadedRoute?.geometry.orEmpty()
        }

    return SimulationMapContent(
        markers = markers,
        polylinePoints = mainPolylinePoints,
        traveledPolylinePoints = traveledPolylinePoints,
        sessionDistanceTraveledMeters = sessionDistanceTraveledMeters,
    )
}
