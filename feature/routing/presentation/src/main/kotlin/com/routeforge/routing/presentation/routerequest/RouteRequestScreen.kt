package com.routeforge.routing.presentation.routerequest

import android.content.Context
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.coredomain.TourProgressRepository
import com.routeforge.coredomain.TourReplayCoordinator
import com.routeforge.coredomain.model.TourSection
import com.routeforge.designsystem.components.NoOpTourProgressRepository
import com.routeforge.designsystem.components.TourHost
import com.routeforge.designsystem.components.TourLabels
import com.routeforge.designsystem.map.RouteForgeMap
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.presentation.R
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditAction
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditEvent
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditState
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val JSON_MIME_TYPE = "application/json"
private const val GPX_MIME_TYPE = "application/gpx+xml"

@Composable
internal fun RouteRequestRoot(
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
internal fun RouteRequestScreen(
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

    Scaffold { paddingValues ->
        TourHost(
            section = TourSection.ROUTE_CREATION,
            steps = rememberRouteRequestTourSteps(state),
            progressRepository = tourProgressRepository,
            replayCoordinator = tourReplayCoordinator,
            labels =
                TourLabels(
                    skip = stringResource(R.string.routing_tour_skip_button),
                    next = stringResource(R.string.routing_tour_next_button),
                    done = stringResource(R.string.routing_tour_done_button),
                    continueMessage = stringResource(R.string.routing_tour_continue_message),
                ),
            isTopMessageVisible = state.errorType != null || showWaypointsUpdatedConfirmation,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
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
        }
    }

    RouteRequestDialogs(state = state, onAction = onAction)
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
