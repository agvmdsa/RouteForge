package com.routeforge.routing.presentation.savedroutes

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.coredomain.TourProgressRepository
import com.routeforge.coredomain.TourReplayCoordinator
import com.routeforge.coredomain.model.FavoriteRoute
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.model.TourSection
import com.routeforge.designsystem.components.ConfirmationBottomSheet
import com.routeforge.designsystem.components.IconBadge
import com.routeforge.designsystem.components.LocalTourTargetRegistry
import com.routeforge.designsystem.components.NoOpTourProgressRepository
import com.routeforge.designsystem.components.SpotlightOverlay
import com.routeforge.designsystem.components.TourStep
import com.routeforge.designsystem.components.TwoButtonConfirmationSheet
import com.routeforge.designsystem.components.rememberTourTargetRegistry
import com.routeforge.designsystem.components.tourTarget
import com.routeforge.designsystem.sheets.MissingRegionsWarningSheet
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.designsystem.theme.Spacing
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.presentation.R
import com.routeforge.routing.presentation.routerequest.ModeChoiceSheet
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val CardSpacing = 12.dp
private val CardShape = RoundedCornerShape(20.dp)
private val CardPadding = 16.dp
private val EmptyStateIconBadgeSize = 72.dp
private val UseSpinnerSize = 24.dp
private const val JSON_MIME_TYPE = "application/json"
private const val GPX_MIME_TYPE = "application/gpx+xml"

@Composable
fun SavedRoutesRoot(
    onDone: () -> Unit,
    onOpenRegionCatalog: () -> Unit,
    viewModel: SavedRoutesViewModel = koinViewModel(),
    tourProgressRepository: TourProgressRepository = koinInject(),
    tourReplayCoordinator: TourReplayCoordinator = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }

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
                SavedRoutesEvent.NavigateBack -> onDone()
                SavedRoutesEvent.NavigateToRegionCatalog -> onOpenRegionCatalog()
                is SavedRoutesEvent.ExportReady -> {
                    pendingExportBytes = event.bytes
                    when (event.format) {
                        RouteFileFormat.JSON -> exportJsonLauncher.launch("route.json")
                        RouteFileFormat.GPX -> exportGpxLauncher.launch("route.gpx")
                    }
                }
            }
        }
    }
    SavedRoutesScreen(
        state = state,
        onAction = viewModel::onAction,
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
fun SavedRoutesScreen(
    state: SavedRoutesState,
    onAction: (SavedRoutesAction) -> Unit,
    tourProgressRepository: TourProgressRepository = remember { NoOpTourProgressRepository() },
    tourReplayCoordinator: TourReplayCoordinator = remember { TourReplayCoordinator() },
) {
    val tourTargetRegistry = rememberTourTargetRegistry()
    val seenTourSections by tourProgressRepository.observeSeenSections().collectAsStateWithLifecycle()
    var activeTourStepIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(seenTourSections) {
        if (TourSection.SAVED !in seenTourSections) activeTourStepIndex = 0
    }

    LaunchedEffect(tourReplayCoordinator) {
        tourReplayCoordinator.replayRequests.collect { section ->
            if (section == TourSection.SAVED) activeTourStepIndex = 0
        }
    }

    val tourSteps = rememberSavedRoutesTourSteps()
    val availableTourSteps = tourSteps.filter { tourTargetRegistry.isTargetAvailable(it.targetId) }

    fun endSavedTour() {
        activeTourStepIndex = null
        tourProgressRepository.markSeen(TourSection.SAVED)
        tourReplayCoordinator.notifyReplayFinished(TourSection.SAVED)
    }

    CompositionLocalProvider(LocalTourTargetRegistry provides tourTargetRegistry) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    Text(
                        text = stringResource(R.string.saved_routes_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(Spacing.ScreenContent),
                    )
                    if (state.routes.isEmpty()) {
                        EmptySavedRoutesContent(modifier = Modifier.weight(1f).fillMaxWidth())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(horizontal = Spacing.ScreenContent),
                            verticalArrangement = Arrangement.spacedBy(CardSpacing),
                        ) {
                            itemsIndexed(state.routes, key = { _, route -> route.id }) { index, route ->
                                SavedRouteCard(
                                    route = route,
                                    isComputing = state.isComputingId == route.id,
                                    isTourTarget = index == 0,
                                    onUseClick = { onAction(SavedRoutesAction.OnUseClick(route.id)) },
                                    onEditClick = { onAction(SavedRoutesAction.OnEditClick(route.id)) },
                                    onDeleteClick = { onAction(SavedRoutesAction.OnDeleteClick(route.id)) },
                                    onExportClick = { onAction(SavedRoutesAction.OnExportClick(route.id)) },
                                )
                            }
                        }
                    }
                }
            }

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
                            endSavedTour()
                        }
                    },
                    onSkip = { endSavedTour() },
                )
            }
        }
    }

    state.routeOptions?.let { options ->
        ModeChoiceSheet(
            options = options,
            onChoose = { mode -> onAction(SavedRoutesAction.OnChooseMode(mode)) },
            onDismiss = { onAction(SavedRoutesAction.OnDismissModeChoice) },
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
            onGoToDownloads = { onAction(SavedRoutesAction.OnOpenRegionCatalog) },
            onContinueAnyway = { onAction(SavedRoutesAction.OnProceedDespiteMissingRegions) },
            onDismiss = { onAction(SavedRoutesAction.OnDismissMissingRegionsWarning) },
        )
    }

    if (state.editingId != null) {
        ModalBottomSheet(
            onDismissRequest = { onAction(SavedRoutesAction.OnDismissEdit) },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(Spacing.ScreenContent), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.saved_routes_edit_title), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.editNameInput,
                    onValueChange = { onAction(SavedRoutesAction.OnEditNameChange(it)) },
                    label = { Text(stringResource(R.string.routing_route_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.editError != null) {
                    Text(text = stringResource(R.string.saved_routes_error_invalid_name), color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAction(SavedRoutesAction.OnConfirmEdit) }) { Text(stringResource(R.string.routing_save_button)) }
                    TextButton(onClick = { onAction(SavedRoutesAction.OnDismissEdit) }) {
                        Text(stringResource(R.string.routing_cancel_button))
                    }
                }
            }
        }
    }

    if (state.pendingDeleteId != null) {
        TwoButtonConfirmationSheet(
            title = stringResource(R.string.saved_routes_delete_confirm_title),
            confirmLabel = stringResource(R.string.routing_delete_button),
            dismissLabel = stringResource(R.string.routing_cancel_button),
            onConfirm = { onAction(SavedRoutesAction.OnConfirmDelete) },
            onDismiss = { onAction(SavedRoutesAction.OnDismissDelete) },
        )
    }

    if (state.pendingExportRouteId != null) {
        ConfirmationBottomSheet(
            title = stringResource(R.string.saved_routes_export_title),
            onDismiss = { onAction(SavedRoutesAction.OnDismissExportFormat) },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onAction(SavedRoutesAction.OnChooseExportFormat(RouteFileFormat.JSON)) }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Text(stringResource(R.string.routing_export_json_button))
                }
                TextButton(onClick = { onAction(SavedRoutesAction.OnChooseExportFormat(RouteFileFormat.GPX)) }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Text(stringResource(R.string.routing_export_gpx_button))
                }
            }
        }
    }
}

/** Every potential Saved-tour step (spec 013 FR-003). Only the first route card in the list
 *  registers these targets ([SavedRouteCard]'s `isTourTarget`) — with a variable-length list each
 *  card would otherwise register the same id, and whichever renders last would silently win. These
 *  are fuller explanations written for a first-time visitor, not the controls' short accessible
 *  names. */
@Composable
private fun rememberSavedRoutesTourSteps(): List<TourStep> =
    listOf(
        TourStep("saved_edit", stringResource(R.string.saved_routes_tour_desc_edit)),
        TourStep("saved_delete", stringResource(R.string.saved_routes_tour_desc_delete)),
        TourStep("saved_export", stringResource(R.string.saved_routes_tour_desc_export)),
        TourStep("saved_use", stringResource(R.string.saved_routes_tour_desc_use)),
    )

@Composable
private fun SavedRouteCard(
    route: FavoriteRoute,
    isComputing: Boolean,
    isTourTarget: Boolean,
    onUseClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(CardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.saved_routes_waypoint_count, route.points.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            IconButton(
                onClick = onEditClick,
                modifier = if (isTourTarget) Modifier.tourTarget("saved_edit") else Modifier,
            ) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.favorites_edit_button))
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = if (isTourTarget) Modifier.tourTarget("saved_delete") else Modifier,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.routing_delete_button))
            }
            IconButton(
                onClick = onExportClick,
                modifier = if (isTourTarget) Modifier.tourTarget("saved_export") else Modifier,
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = stringResource(R.string.saved_routes_export_button))
            }
            IconButton(
                onClick = onUseClick,
                modifier = if (isTourTarget) Modifier.tourTarget("saved_use") else Modifier,
            ) {
                if (isComputing) {
                    CircularProgressIndicator(modifier = Modifier.size(UseSpinnerSize))
                } else {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = stringResource(R.string.saved_routes_use_button))
                }
            }
        }
    }
}

@Composable
private fun EmptySavedRoutesContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadge(icon = Icons.Filled.Bookmark, size = EmptyStateIconBadgeSize)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.saved_routes_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.saved_routes_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun SavedRoutesScreenPreview() {
    RouteForgeTheme {
        SavedRoutesScreen(
            state =
                SavedRoutesState(
                    routes =
                        listOf(
                            FavoriteRoute(id = "1", name = "Morning loop", points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0))),
                        ),
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SavedRoutesScreenEmptyPreview() {
    RouteForgeTheme {
        SavedRoutesScreen(state = SavedRoutesState(), onAction = {})
    }
}
