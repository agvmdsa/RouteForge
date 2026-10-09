package com.routeforge.routing.presentation.routerequest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.coredomain.model.RouteOptions
import com.routeforge.routing.presentation.R

private val ScreenContentPadding = 16.dp
private val ControlsRowSpacing = 8.dp
private val SwitchModeSpinnerSize = 24.dp

@Composable
internal fun ModeChoiceSheet(
    options: RouteOptions,
    onChoose: (RoutePlaybackMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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
    isComputingPreview: Boolean,
    onExport: (RouteFileFormat) -> Unit,
    onUseRoute: () -> Unit,
    onSwitchMode: () -> Unit,
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
            TextButton(onClick = onSwitchMode, modifier = Modifier.fillMaxWidth(), enabled = !isComputingPreview) {
                if (isComputingPreview) {
                    CircularProgressIndicator(modifier = Modifier.size(SwitchModeSpinnerSize))
                } else {
                    Icon(
                        imageVector = if (route.mode == RoutePlaybackMode.GUIDED) Icons.Filled.Straighten else Icons.Filled.Signpost,
                        contentDescription = null,
                    )
                }
                Text(stringResource(R.string.routing_switch_route_mode_button))
            }
            Button(onClick = onUseRoute, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Check, contentDescription = null)
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
internal fun SaveRouteSheet(
    nameInput: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationBottomSheet(title = stringResource(R.string.routing_save_route_title), onDismiss = onDismiss) {
        OutlinedTextField(
            value = nameInput,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.routing_route_name_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
            Button(onClick = onConfirm, enabled = nameInput.isNotBlank()) {
                Text(stringResource(R.string.routing_save_route_confirm_button))
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.routing_add_waypoint_cancel_button)) }
        }
    }
}
