package com.routeforge.routing.presentation

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.designsystem.map.RouteForgeMap
import com.routeforge.designsystem.map.RouteForgeMapMarker
import com.routeforge.designsystem.map.RouteForgeMapMarkerIcon
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.routing.domain.model.RequiredRegionsSummary
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.model.RouteOptions
import org.koin.androidx.compose.koinViewModel

private val ScreenContentPadding = 16.dp
private val ControlsRowSpacing = 8.dp
private const val JSON_MIME_TYPE = "application/json"
private const val GPX_MIME_TYPE = "application/gpx+xml"

@Composable
fun RouteRequestRoot(
    onRouteComputed: (Route) -> Unit,
    onOpenRegionCatalog: () -> Unit,
    viewModel: RouteRequestViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
            val format =
                if (uri.lastPathSegment?.lowercase()?.endsWith(".gpx") == true) RouteFileFormat.GPX else RouteFileFormat.JSON
            viewModel.onAction(RouteRequestAction.OnRouteFileImported(bytes, format))
        }
    val exportJsonLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) { uri ->
            writePendingExport(context, uri, pendingExportBytes)
            pendingExportBytes = null
        }
    val exportGpxLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(GPX_MIME_TYPE)) { uri ->
            writePendingExport(context, uri, pendingExportBytes)
            pendingExportBytes = null
        }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RouteRequestEvent.RouteComputed -> onRouteComputed(event.route)
                RouteRequestEvent.NavigateToRegionCatalog -> onOpenRegionCatalog()
                is RouteRequestEvent.ExportReady -> {
                    pendingExportBytes = event.bytes
                    when (event.format) {
                        RouteFileFormat.JSON -> exportJsonLauncher.launch("route.json")
                        RouteFileFormat.GPX -> exportGpxLauncher.launch("route.gpx")
                    }
                }
            }
        }
    }

    RouteRequestScreen(
        state = state,
        onAction = viewModel::onAction,
        onImportClick = { importLauncher.launch(arrayOf("*/*")) },
    )
}

private fun writePendingExport(
    context: Context,
    uri: android.net.Uri?,
    bytes: ByteArray?,
) {
    if (uri == null || bytes == null) return
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
}

@Composable
fun RouteRequestScreen(
    state: RouteRequestState,
    onAction: (RouteRequestAction) -> Unit,
    onImportClick: () -> Unit = {},
) {
    var hasCenteredOnce by remember { mutableStateOf(false) }
    var cameraTarget by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(state.draft.points.size, state.lastKnownLocation) {
        if (hasCenteredOnce) return@LaunchedEffect
        val first = state.draft.points.firstOrNull()
        val target =
            first?.let { it.latitude to it.longitude }
                ?: state.lastKnownLocation?.let { it.latitude to it.longitude }
        if (target != null) {
            cameraTarget = target
            hasCenteredOnce = true
        }
    }

    val markerBackground = MaterialTheme.colorScheme.primary.toArgb()
    val markerText = MaterialTheme.colorScheme.onPrimary.toArgb()
    val markers =
        state.draft.points.mapIndexed { index, point ->
            RouteForgeMapMarker(
                id = index,
                latitude = point.latitude,
                longitude = point.longitude,
                icon = RouteForgeMapMarkerIcon.Numbered(index + 1, markerBackground, markerText),
                draggable = true,
                onClick = { onAction(RouteRequestAction.OnMarkerClick(index)) },
                onDragEnd = { latitude, longitude -> onAction(RouteRequestAction.OnMarkerDragged(index, latitude, longitude)) },
            )
        }
    val polylinePoints = state.route?.geometry ?: state.draft.points.map { it.latitude to it.longitude }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            RouteForgeMap(
                markers = markers,
                polylinePoints = polylinePoints,
                cameraTarget = cameraTarget,
                onMapTap = { latitude, longitude -> onAction(RouteRequestAction.OnMapTap(latitude, longitude)) },
                modifier = Modifier.fillMaxSize(),
            )

            RouteStatusPill(
                state = state,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = ScreenContentPadding),
            )

            state.errorType?.let { errorType ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 72.dp, start = ScreenContentPadding, end = ScreenContentPadding),
                ) {
                    Text(text = errorType.toMessage(), modifier = Modifier.padding(12.dp))
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ControlsRowSpacing),
                modifier = Modifier.align(Alignment.BottomStart).padding(ScreenContentPadding),
            ) {
                FloatingActionButton(onClick = onImportClick) {
                    Icon(Icons.Filled.FileUpload, contentDescription = stringResource(R.string.routing_import_button))
                }
                FloatingActionButton(onClick = { onAction(RouteRequestAction.OnUndo) }) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.routing_undo_button))
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ControlsRowSpacing),
                modifier = Modifier.align(Alignment.BottomEnd).padding(ScreenContentPadding),
            ) {
                if (state.draft.points.size >= MIN_WAYPOINTS_TO_PLAY) {
                    FloatingActionButton(
                        onClick = { onAction(RouteRequestAction.OnRequestRoute) },
                    ) {
                        if (state.isComputing) {
                            CircularProgressIndicator()
                        } else {
                            Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.routing_proceed_button))
                        }
                    }
                }
                FloatingActionButton(onClick = { onAction(RouteRequestAction.OnOpenRegionCatalog) }) {
                    Icon(Icons.Filled.Map, contentDescription = stringResource(R.string.routing_manage_regions_button))
                }
            }
        }
    }

    state.routeOptions?.let { options ->
        ModeChoiceSheet(options = options, onChoose = { mode -> onAction(RouteRequestAction.OnChooseMode(mode)) })
    }

    state.route?.let { route ->
        var isDismissed by remember(route) { mutableStateOf(false) }
        if (!isDismissed) {
            RouteReadySheet(
                route = route,
                chosenMode = state.chosenMode,
                onExport = { format -> onAction(RouteRequestAction.OnExportRoute(format)) },
                onUseRoute = { onAction(RouteRequestAction.OnUseRoute) },
                onDismiss = { isDismissed = true },
            )
        }
    }

    if (state.editingIndex != null) {
        EditWaypointSheet(
            latitude = state.editLatitudeInput,
            longitude = state.editLongitudeInput,
            onLatitudeChange = { onAction(RouteRequestAction.OnEditLatitudeChange(it)) },
            onLongitudeChange = { onAction(RouteRequestAction.OnEditLongitudeChange(it)) },
            onConfirm = { onAction(RouteRequestAction.OnConfirmEdit) },
            onDelete = { onAction(RouteRequestAction.OnDeleteEditingWaypoint) },
            onDismiss = { onAction(RouteRequestAction.OnDismissEdit) },
        )
    }

    state.missingRegionsWarning?.let { summary ->
        MissingRegionsWarningSheet(
            summary = summary,
            onGoToDownloads = { onAction(RouteRequestAction.OnOpenRegionCatalog) },
            onContinueAnyway = { onAction(RouteRequestAction.OnProceedDespiteMissingRegions) },
            onDismiss = { onAction(RouteRequestAction.OnDismissMissingRegionsWarning) },
        )
    }
}

@Composable
private fun RouteStatusPill(
    state: RouteRequestState,
    modifier: Modifier = Modifier,
) {
    val text =
        state.route?.let { route ->
            stringResource(
                R.string.routing_route_ready_label,
                (state.chosenMode ?: route.mode).name,
                route.distanceMeters.toInt(),
                route.geometry.size,
            )
        } ?: pluralPointsLabel(state.draft.points.size)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
private fun pluralPointsLabel(count: Int): String = stringResource(R.string.routing_points_count_label, count)

@Composable
private fun ModeChoiceSheet(
    options: RouteOptions,
    onChoose: (RoutePlaybackMode) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
        Column(modifier = Modifier.padding(ScreenContentPadding), verticalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
            Text(stringResource(R.string.routing_choose_mode_label), style = MaterialTheme.typography.titleMedium)
            if (options.guided != null) {
                Button(onClick = { onChoose(RoutePlaybackMode.GUIDED) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.routing_mode_guided_button))
                }
            }
            Button(onClick = { onChoose(RoutePlaybackMode.FREE_ROAM) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.routing_mode_free_roam_button))
            }
        }
    }
}

@Composable
private fun RouteReadySheet(
    route: Route,
    chosenMode: RoutePlaybackMode?,
    onExport: (RouteFileFormat) -> Unit,
    onUseRoute: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(ScreenContentPadding), verticalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
            Text(
                text =
                    stringResource(
                        R.string.routing_route_ready_label,
                        (chosenMode ?: route.mode).name,
                        route.distanceMeters.toInt(),
                        route.geometry.size,
                    ),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
                TextButton(onClick = { onExport(RouteFileFormat.JSON) }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Text(stringResource(R.string.routing_export_json_button))
                }
                TextButton(onClick = { onExport(RouteFileFormat.GPX) }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Text(stringResource(R.string.routing_export_gpx_button))
                }
            }
            Button(onClick = onUseRoute, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.routing_use_route_button))
            }
        }
    }
}

@Composable
private fun EditWaypointSheet(
    latitude: String,
    longitude: String,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(ScreenContentPadding), verticalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
            Text(stringResource(R.string.routing_edit_waypoint_title), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
                OutlinedTextField(
                    value = latitude,
                    onValueChange = onLatitudeChange,
                    label = { Text(stringResource(R.string.routing_latitude_label)) },
                    modifier = Modifier.fillMaxWidth(0.5f),
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = onLongitudeChange,
                    label = { Text(stringResource(R.string.routing_longitude_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
                Button(onClick = onConfirm) { Text(stringResource(R.string.routing_save_button)) }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(stringResource(R.string.routing_delete_button))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.routing_cancel_button))
                }
            }
        }
    }
}

@Composable
private fun MissingRegionsWarningSheet(
    summary: RequiredRegionsSummary,
    onGoToDownloads: () -> Unit,
    onContinueAnyway: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(ScreenContentPadding), verticalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
            Text(stringResource(R.string.routing_missing_regions_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.routing_missing_regions_message,
                    summary.totalMissingBytes / 1_000_000,
                    summary.regions.joinToString { it.displayName },
                ),
            )
            if (summary.uncoveredWaypointCount > 0) {
                Text(
                    text = stringResource(R.string.routing_missing_regions_uncovered_note, summary.uncoveredWaypointCount),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
                Button(onClick = onGoToDownloads) { Text(stringResource(R.string.routing_missing_regions_go_to_downloads)) }
                TextButton(onClick = onContinueAnyway) { Text(stringResource(R.string.routing_missing_regions_continue_anyway)) }
            }
        }
    }
}

@Composable
private fun RouteRequestError.toMessage(): String =
    when (this) {
        RouteRequestError.INVALID_EDIT_COORDINATES -> stringResource(R.string.routing_error_invalid_edit_coordinates)
        RouteRequestError.TOO_FEW_WAYPOINTS_TO_PLAY -> stringResource(R.string.routing_error_too_few_waypoints)
        RouteRequestError.IMPORT_TOO_FEW_WAYPOINTS -> stringResource(R.string.routing_error_import_too_few_waypoints)
        RouteRequestError.IMPORT_COORDINATE_OUT_OF_RANGE -> stringResource(R.string.routing_error_import_coordinate_out_of_range)
        RouteRequestError.IMPORT_MALFORMED -> stringResource(R.string.routing_error_import_malformed)
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
            state = RouteRequestState(errorType = RouteRequestError.TOO_FEW_WAYPOINTS_TO_PLAY),
            onAction = {},
        )
    }
}
