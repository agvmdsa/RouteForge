package com.routeforge.routing.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.designsystem.components.ConfirmationBottomSheet
import com.routeforge.routing.domain.model.RequiredRegionsSummary
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.model.RouteOptions

private val ScreenContentPadding = 16.dp
private val ControlsRowSpacing = 8.dp

@Composable
internal fun ModeChoiceSheet(
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
internal fun RouteReadySheet(
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
internal fun EditWaypointSheet(
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
internal fun MissingRegionsWarningSheet(
    summary: RequiredRegionsSummary,
    onGoToDownloads: () -> Unit,
    onContinueAnyway: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationBottomSheet(title = stringResource(R.string.routing_missing_regions_title), onDismiss = onDismiss) {
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
