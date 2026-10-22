package com.routeforge.routing.presentation.routerequest

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

/** Centers the map on the draft's first waypoint (or the last known real location, if the draft is
 *  still empty) exactly once — [hasCenteredOnce] stops a later edit from re-centering under the user. */
@Composable
internal fun rememberInitialCameraTarget(state: RouteRequestState): LatLng? {
    var hasCenteredOnce by remember { mutableStateOf(false) }
    var cameraTarget by remember { mutableStateOf<LatLng?>(null) }
    LaunchedEffect(state.draft.points.size, state.lastKnownLocation) {
        if (hasCenteredOnce) return@LaunchedEffect
        val first = state.draft.points.firstOrNull()
        val target =
            first?.let { LatLng(it.latitude, it.longitude) }
                ?: state.lastKnownLocation?.let { LatLng(it.latitude, it.longitude) }
        if (target != null) {
            cameraTarget = target
            hasCenteredOnce = true
        }
    }
    return cameraTarget
}

/** Builds the map's markers and polyline, substituting a marker's live drag position into the
 *  polyline while it's being dragged so the line visibly follows the gesture before [onAction]'s
 *  [RouteRequestAction.OnMarkerDragged] commits it. */
@Composable
internal fun rememberRouteMarkersAndPolyline(
    state: RouteRequestState,
    onAction: (RouteRequestAction) -> Unit,
): Pair<List<RouteForgeMapMarker>, List<LatLng>> {
    val markerBackground = MaterialTheme.colorScheme.primary.toArgb()
    val markerText = MaterialTheme.colorScheme.onPrimary.toArgb()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var draggingPosition by remember { mutableStateOf<LatLng?>(null) }
    val markers =
        state.draft.points.mapIndexed { index, point ->
            RouteForgeMapMarker(
                id = index,
                latitude = point.latitude,
                longitude = point.longitude,
                icon = RouteForgeMapMarkerIcon.Numbered(index + 1, markerBackground, markerText),
                draggable = true,
                onClick = { onAction(RouteRequestAction.OnMarkerClick(index)) },
                onDrag = { latitude, longitude ->
                    draggingIndex = index
                    draggingPosition = LatLng(latitude, longitude)
                },
                onDragEnd = { latitude, longitude ->
                    draggingIndex = null
                    draggingPosition = null
                    onAction(RouteRequestAction.OnMarkerDragged(index, latitude, longitude))
                },
            )
        }
    val liveDraggingPosition = draggingPosition
    val polylinePoints =
        if (draggingIndex != null && liveDraggingPosition != null) {
            state.draft.points.mapIndexed { i, p -> if (i == draggingIndex) liveDraggingPosition else LatLng(p.latitude, p.longitude) }
        } else {
            state.route?.geometry ?: state.previewRoute?.geometry ?: state.draft.points.map { LatLng(it.latitude, it.longitude) }
        }
    return markers to polylinePoints
}
