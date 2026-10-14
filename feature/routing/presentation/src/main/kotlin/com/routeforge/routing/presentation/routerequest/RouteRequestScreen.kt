package com.routeforge.routing.presentation.routerequest

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.coredomain.TourProgressRepository
import com.routeforge.coredomain.TourReplayCoordinator
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.TourSection
import com.routeforge.designsystem.components.AutoDismissConfirmationPill
import com.routeforge.designsystem.components.FabColumn
import com.routeforge.designsystem.components.LocalTourTargetRegistry
import com.routeforge.designsystem.components.NoOpTourProgressRepository
import com.routeforge.designsystem.components.SpotlightOverlay
import com.routeforge.designsystem.components.TopBanner
import com.routeforge.designsystem.components.TourStep
import com.routeforge.designsystem.components.rememberTourTargetRegistry
import com.routeforge.designsystem.components.tourTarget
import com.routeforge.designsystem.map.RouteForgeMap
import com.routeforge.designsystem.map.RouteForgeMapMarker
import com.routeforge.designsystem.map.RouteForgeMapMarkerIcon
import com.routeforge.designsystem.sheets.AddWaypointConfirmationSheet
import com.routeforge.designsystem.sheets.MissingRegionsWarningSheet
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.designsystem.theme.Spacing
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditAction
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditEvent
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditState
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditViewModel
import com.routeforge.routing.presentation.R
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val JSON_MIME_TYPE = "application/json"
private const val GPX_MIME_TYPE = "application/gpx+xml"

@Composable
fun RouteRequestRoot(
    onGoToSimulate: () -> Unit,
    onOpenRegionCatalog: () -> Unit,
    onOpenFavorites: () -> Unit,
    viewModel: RouteRequestViewModel = koinViewModel(),
    waypointEditViewModel: WaypointEditViewModel = koinViewModel(),
    tourProgressRepository: TourProgressRepository = koinInject(),
    tourReplayCoordinator: TourReplayCoordinator = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editState by waypointEditViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showWaypointsUpdatedConfirmation by remember { mutableStateOf(false) }

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
                RouteRequestEvent.GoToSimulate -> onGoToSimulate()
                RouteRequestEvent.NavigateToRegionCatalog -> onOpenRegionCatalog()
                RouteRequestEvent.NavigateToFavorites -> onOpenFavorites()
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

    LaunchedEffect(waypointEditViewModel) {
        waypointEditViewModel.events.collect { event ->
            when (event) {
                is WaypointEditEvent.Committed -> viewModel.applyEditedWaypoints(event.points)
                WaypointEditEvent.GoToDownloads -> onOpenRegionCatalog()
                WaypointEditEvent.WaypointsUpdated -> showWaypointsUpdatedConfirmation = true
            }
        }
    }

    RouteRequestScreen(
        state = state,
        onAction = viewModel::onAction,
        onImportClick = { importLauncher.launch(arrayOf("*/*")) },
        editState = editState,
        onEditAction = waypointEditViewModel::onAction,
        showWaypointsUpdatedConfirmation = showWaypointsUpdatedConfirmation,
        onDismissWaypointsUpdatedConfirmation = { showWaypointsUpdatedConfirmation = false },
        tourProgressRepository = tourProgressRepository,
        tourReplayCoordinator = tourReplayCoordinator,
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
    editState: WaypointEditState = WaypointEditState(),
    onEditAction: (WaypointEditAction) -> Unit = {},
    showWaypointsUpdatedConfirmation: Boolean = false,
    onDismissWaypointsUpdatedConfirmation: () -> Unit = {},
    tourProgressRepository: TourProgressRepository = remember { NoOpTourProgressRepository() },
    tourReplayCoordinator: TourReplayCoordinator = remember { TourReplayCoordinator() },
) {
    val cameraTarget = rememberInitialCameraTarget(state)
    val (markers, polylinePoints) = rememberRouteMarkersAndPolyline(state, onAction)
    val tourTargetRegistry = rememberTourTargetRegistry()
    val seenTourSections by tourProgressRepository.observeSeenSections().collectAsStateWithLifecycle()
    var activeTourStepIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(seenTourSections) {
        if (TourSection.ROUTE_CREATION !in seenTourSections) activeTourStepIndex = 0
    }

    LaunchedEffect(tourReplayCoordinator) {
        tourReplayCoordinator.replayRequests.collect { section ->
            if (section == TourSection.ROUTE_CREATION) activeTourStepIndex = 0
        }
    }

    val tourSteps = rememberRouteRequestTourSteps()
    val availableTourSteps = tourSteps.filter { tourTargetRegistry.isTargetAvailable(it.targetId) }

    fun endRouteCreationTour() {
        activeTourStepIndex = null
        tourProgressRepository.markSeen(TourSection.ROUTE_CREATION)
        tourReplayCoordinator.notifyReplayFinished(TourSection.ROUTE_CREATION)
    }

    CompositionLocalProvider(LocalTourTargetRegistry provides tourTargetRegistry) {
        Scaffold { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                RouteForgeMap(
                    markers = markers,
                    polylinePoints = polylinePoints,
                    cameraTarget = cameraTarget,
                    mapStyle = state.mapStyle,
                    onMapTap = { latitude, longitude -> onAction(RouteRequestAction.OnMapTap(latitude, longitude)) },
                    modifier = Modifier.fillMaxSize(),
                )

                RouteRequestOverlays(
                    state = state,
                    showWaypointsUpdatedConfirmation = showWaypointsUpdatedConfirmation,
                    onDismissWaypointsUpdatedConfirmation = onDismissWaypointsUpdatedConfirmation,
                )

                RouteRequestLeadingFabs(
                    state = state,
                    onAction = onAction,
                    onImportClick = onImportClick,
                    editState = editState,
                    onEditAction = onEditAction,
                )

                RouteRequestTrailingFabs(state = state, onAction = onAction)

                activeTourStepIndex?.let { index ->
                    val clampedIndex = index.coerceIn(0, (availableTourSteps.size - 1).coerceAtLeast(0))
                    SpotlightOverlay(
                        steps = availableTourSteps,
                        currentStepIndex = clampedIndex,
                        registry = tourTargetRegistry,
                        skipButtonLabel = stringResource(R.string.routing_tour_skip_button),
                        nextButtonLabel = stringResource(R.string.routing_tour_next_button),
                        doneButtonLabel = stringResource(R.string.routing_tour_done_button),
                        onNext = {
                            if (clampedIndex + 1 < availableTourSteps.size) {
                                activeTourStepIndex = clampedIndex + 1
                            } else {
                                endRouteCreationTour()
                            }
                        },
                        onSkip = { endRouteCreationTour() },
                    )
                }
            }
        }
    }

    RouteRequestDialogs(state = state, onAction = onAction)
}

/** Every potential Route Creation-tour step (spec 013 FR-002), filtered down by
 *  [RouteRequestScreen] to whichever targets are actually registered before being handed to
 *  [SpotlightOverlay] — e.g. Switch route mode only exists before a route has been computed. These
 *  are fuller explanations written for a first-time visitor, not the controls' short accessible
 *  names. */
@Composable
private fun rememberRouteRequestTourSteps(): List<TourStep> =
    listOf(
        TourStep("routeReq_status_pill", stringResource(R.string.routing_tour_desc_status_pill)),
        TourStep("routeReq_import", stringResource(R.string.routing_tour_desc_import)),
        TourStep("routeReq_add_from_favorites", stringResource(R.string.routing_tour_desc_add_from_favorites)),
        TourStep("routeReq_undo", stringResource(R.string.routing_tour_desc_undo)),
        TourStep("routeReq_edit_waypoints", stringResource(R.string.routing_tour_desc_edit_waypoints)),
        TourStep("routeReq_switch_mode", stringResource(R.string.routing_tour_desc_switch_mode)),
        TourStep("routeReq_proceed", stringResource(R.string.routing_tour_desc_proceed)),
        TourStep("routeReq_save_route", stringResource(R.string.routing_tour_desc_save_route)),
    )

/** Centers the map on the draft's first waypoint (or the last known real location, if the draft is
 *  still empty) exactly once — [hasCenteredOnce] stops a later edit from re-centering under the user. */
@Composable
private fun rememberInitialCameraTarget(state: RouteRequestState): LatLng? {
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
private fun rememberRouteMarkersAndPolyline(
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

/** Status pill, error banner, and the transient "waypoints updated" confirmation — the map's
 *  top-anchored overlays, as opposed to the bottom-anchored FAB columns. */
@Composable
private fun BoxScope.RouteRequestOverlays(
    state: RouteRequestState,
    showWaypointsUpdatedConfirmation: Boolean,
    onDismissWaypointsUpdatedConfirmation: () -> Unit,
) {
    RouteStatusPill(
        state = state,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = Spacing.ScreenContent),
    )

    state.errorType?.let { errorType ->
        TopBanner(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.BannerTopOffset, start = Spacing.ScreenContent, end = Spacing.ScreenContent),
        ) {
            Text(text = errorType.toMessage())
        }
    }

    AutoDismissConfirmationPill(
        message = stringResource(R.string.routing_waypoints_updated_message),
        isVisible = showWaypointsUpdatedConfirmation,
        onDismiss = onDismissWaypointsUpdatedConfirmation,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = Spacing.BannerTopOffset),
    )
}

/** Bottom-start FAB column: import, add-from-favorites, undo (when available), and the waypoint
 *  reorder/edit sheet trigger. */
@Composable
private fun BoxScope.RouteRequestLeadingFabs(
    state: RouteRequestState,
    onAction: (RouteRequestAction) -> Unit,
    onImportClick: () -> Unit,
    editState: WaypointEditState,
    onEditAction: (WaypointEditAction) -> Unit,
) {
    FabColumn(alignment = Alignment.BottomStart) {
        FloatingActionButton(onClick = onImportClick, modifier = Modifier.tourTarget("routeReq_import")) {
            Icon(Icons.Filled.FileUpload, contentDescription = stringResource(R.string.routing_import_button))
        }
        FloatingActionButton(
            onClick = { onAction(RouteRequestAction.OnOpenFavoritesClick) },
            modifier = Modifier.tourTarget("routeReq_add_from_favorites"),
        ) {
            Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.routing_add_from_favorites_button))
        }
        if (state.draft.canUndo) {
            FloatingActionButton(
                onClick = { onAction(RouteRequestAction.OnUndo) },
                modifier = Modifier.tourTarget("routeReq_undo"),
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.routing_undo_button))
            }
        }
        WaypointEditSection(
            points = state.draft.points,
            chosenMode = state.chosenMode ?: state.route?.mode,
            editState = editState,
            onEditAction = onEditAction,
        )
    }
}

/** Bottom-end FAB column: mode-switch preview, the proceed/compute button, and save-route —
 *  all gated on there being enough waypoints to play at all. */
@Composable
private fun BoxScope.RouteRequestTrailingFabs(
    state: RouteRequestState,
    onAction: (RouteRequestAction) -> Unit,
) {
    FabColumn(alignment = Alignment.BottomEnd) {
        if (state.draft.points.size >= MIN_WAYPOINTS_TO_PLAY) {
            if (state.route == null) {
                FloatingActionButton(
                    onClick = { onAction(RouteRequestAction.OnSwitchRouteModeClick) },
                    modifier = Modifier.tourTarget("routeReq_switch_mode"),
                ) {
                    if (state.isComputingPreview) {
                        CircularProgressIndicator()
                    } else {
                        Icon(
                            imageVector = if (state.previewMode == RoutePlaybackMode.GUIDED) Icons.Filled.Straighten else Icons.Filled.Signpost,
                            contentDescription = stringResource(R.string.routing_switch_route_mode_button),
                        )
                    }
                }
            }
            FloatingActionButton(
                onClick = { onAction(RouteRequestAction.OnRequestRoute) },
                modifier = Modifier.tourTarget("routeReq_proceed"),
            ) {
                if (state.isComputing) {
                    CircularProgressIndicator()
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.routing_proceed_button))
                }
            }
            FloatingActionButton(
                onClick = { onAction(RouteRequestAction.OnSaveRouteClick) },
                modifier = Modifier.tourTarget("routeReq_save_route"),
            ) {
                Icon(Icons.Filled.Bookmark, contentDescription = stringResource(R.string.routing_save_route_button))
            }
        }
    }
}

/** The screen's modal sheets — each gated on its own nullable/boolean state field, shown at most
 *  one at a time in practice since they correspond to mutually-exclusive user flows. */
@Composable
private fun RouteRequestDialogs(
    state: RouteRequestState,
    onAction: (RouteRequestAction) -> Unit,
) {
    state.route?.let { route ->
        var isDismissed by remember(route) { mutableStateOf(false) }
        if (!isDismissed) {
            RouteReadySheet(
                route = route,
                chosenMode = state.chosenMode,
                isComputingPreview = state.isComputingPreview,
                onExport = { format -> onAction(RouteRequestAction.OnExportRoute(format)) },
                onUseRoute = {
                    isDismissed = true
                    onAction(RouteRequestAction.OnUseRoute)
                },
                onSwitchMode = { onAction(RouteRequestAction.OnSwitchRouteModeClick) },
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
            title = stringResource(R.string.routing_missing_regions_title),
            message =
                stringResource(
                    R.string.routing_missing_regions_message,
                    summary.totalMissingBytes / 1_000_000,
                    summary.regions.joinToString { it.displayName },
                ),
            uncoveredNote =
                if (summary.uncoveredWaypointCount > 0) {
                    stringResource(R.string.routing_missing_regions_uncovered_note, summary.uncoveredWaypointCount)
                } else {
                    null
                },
            goToDownloadsLabel = stringResource(R.string.routing_missing_regions_go_to_downloads),
            continueAnywayLabel = stringResource(R.string.routing_missing_regions_continue_anyway),
            onGoToDownloads = { onAction(RouteRequestAction.OnOpenRegionCatalog) },
            onContinueAnyway = { onAction(RouteRequestAction.OnProceedDespiteMissingRegions) },
            onDismiss = { onAction(RouteRequestAction.OnDismissMissingRegionsWarning) },
        )
    }

    if (state.isSaveRouteSheetOpen) {
        SaveRouteSheet(
            nameInput = state.routeNameInput,
            onNameChange = { onAction(RouteRequestAction.OnRouteNameInputChange(it)) },
            onConfirm = { onAction(RouteRequestAction.OnConfirmSaveRoute) },
            onDismiss = { onAction(RouteRequestAction.OnDismissSaveRoute) },
        )
    }

    val pendingAddLatitude = state.pendingAddLatitude
    val pendingAddLongitude = state.pendingAddLongitude
    if (pendingAddLatitude != null && pendingAddLongitude != null) {
        val pendingAddPlaceName = state.pendingAddPlaceName
        AddWaypointConfirmationSheet(
            title = stringResource(R.string.routing_add_waypoint_title),
            coordinatesMessage =
                if (pendingAddPlaceName != null) {
                    stringResource(R.string.routing_add_waypoint_message_named, pendingAddPlaceName)
                } else {
                    stringResource(R.string.routing_add_waypoint_message, pendingAddLatitude, pendingAddLongitude)
                },
            saveAsFavoriteLabel = stringResource(R.string.routing_save_as_favorite_label),
            favoriteNameLabel = stringResource(R.string.routing_favorite_name_label),
            confirmLabel = stringResource(R.string.routing_add_waypoint_confirm_button),
            cancelLabel = stringResource(R.string.routing_add_waypoint_cancel_button),
            isSaveAsFavoriteChecked = state.isSaveAsFavoriteChecked,
            favoriteNameInput = state.favoriteNameInput,
            onToggleSaveAsFavorite = { onAction(RouteRequestAction.OnToggleSaveAsFavorite) },
            onFavoriteNameChange = { onAction(RouteRequestAction.OnFavoriteNameInputChange(it)) },
            onConfirm = { onAction(RouteRequestAction.OnConfirmAddWaypoint) },
            onDismiss = { onAction(RouteRequestAction.OnDismissAddWaypoint) },
        )
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
        RouteRequestError.MODE_PREVIEW_NO_PATH -> stringResource(R.string.routing_error_mode_preview_no_path)
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
