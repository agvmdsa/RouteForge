package com.routeforge.routing.presentation.savedroutes

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
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
import com.routeforge.coredomain.model.FavoriteRoute
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.designsystem.components.ConfirmationBottomSheet
import com.routeforge.designsystem.components.IconBadge
import com.routeforge.designsystem.sheets.MissingRegionsWarningSheet
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.presentation.R
import com.routeforge.routing.presentation.routerequest.ModeChoiceSheet
import org.koin.androidx.compose.koinViewModel

private val ScreenContentPadding = 16.dp
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
    SavedRoutesScreen(state = state, onAction = viewModel::onAction)
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
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Text(
                text = stringResource(R.string.saved_routes_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(ScreenContentPadding),
            )
            if (state.routes.isEmpty()) {
                EmptySavedRoutesContent(modifier = Modifier.weight(1f).fillMaxWidth())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = ScreenContentPadding),
                    verticalArrangement = Arrangement.spacedBy(CardSpacing),
                ) {
                    items(state.routes, key = { it.id }) { route ->
                        SavedRouteCard(
                            route = route,
                            isComputing = state.isComputingId == route.id,
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
            Column(modifier = Modifier.padding(ScreenContentPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        ConfirmationBottomSheet(
            title = stringResource(R.string.saved_routes_delete_confirm_title),
            onDismiss = { onAction(SavedRoutesAction.OnDismissDelete) },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAction(SavedRoutesAction.OnConfirmDelete) }) { Text(stringResource(R.string.routing_delete_button)) }
                TextButton(onClick = { onAction(SavedRoutesAction.OnDismissDelete) }) {
                    Text(stringResource(R.string.routing_cancel_button))
                }
            }
        }
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

@Composable
private fun SavedRouteCard(
    route: FavoriteRoute,
    isComputing: Boolean,
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
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.favorites_edit_button))
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.routing_delete_button))
            }
            IconButton(onClick = onExportClick) {
                Icon(Icons.Filled.FileDownload, contentDescription = stringResource(R.string.saved_routes_export_button))
            }
            IconButton(onClick = onUseClick) {
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
