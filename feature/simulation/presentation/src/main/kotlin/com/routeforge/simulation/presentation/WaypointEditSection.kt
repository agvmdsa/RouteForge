package com.routeforge.simulation.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.designsystem.format.formatCoordinates
import com.routeforge.designsystem.sheets.AddWaypointConfirmationSheet
import com.routeforge.designsystem.sheets.MissingRegionsWarningSheet
import com.routeforge.designsystem.waypointedit.WaypointEditSheet
import com.routeforge.designsystem.waypointedit.WaypointListItem
import com.routeforge.simulation.presentation.waypointedit.WaypointEditAction
import com.routeforge.simulation.presentation.waypointedit.WaypointEditState

private val WaypointEditTriggerButtonSize = 48.dp

/** The "Edit route" trigger (visible iff a route is loaded and the sheet isn't already open) —
 *  lives inside [com.routeforge.simulation.presentation.components.RouteProgressCard] now; only
 *  this button relocated there, not the sheet it opens (see [WaypointEditOverlays]). */
@Composable
fun WaypointEditTriggerButton(
    loadedRoute: Route?,
    distanceTraveledMeters: Double?,
    editState: WaypointEditState,
    onEditAction: (WaypointEditAction) -> Unit,
    modifier: Modifier = Modifier.size(WaypointEditTriggerButtonSize),
) {
    if (loadedRoute != null && !editState.isOpen) {
        IconButton(
            onClick = { onEditAction(WaypointEditAction.OnOpen(loadedRoute, distanceTraveledMeters)) },
            modifier = modifier,
        ) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.simulation_edit_route_button))
        }
    }
}

/** The waypoint-edit sheet and its own sub-sheets — Simulate's half of spec 008's shared
 *  waypoint-edit surface. Unaffected by spec 011's control relocation: only
 *  [WaypointEditTriggerButton] moved, this stays called from [SimulationScreen] at its original
 *  position so z-order over the map is unchanged. Map-tap/marker-drag gating and rendering stay in
 *  [SimulationScreen] itself, since this composable doesn't own the map. */
@Composable
fun WaypointEditOverlays(
    loadedRoute: Route?,
    editState: WaypointEditState,
    onEditAction: (WaypointEditAction) -> Unit,
) {
    if (editState.isOpen) {
        WaypointEditSheet(
            title = stringResource(R.string.simulation_waypoint_edit_sheet_title),
            items =
                editState.points.mapIndexed { index, point ->
                    WaypointListItem(
                        id = index,
                        label = "${index + 1}. ${formatCoordinates(point.latitude, point.longitude)}",
                        isEditable = index >= editState.firstEditableIndex,
                    )
                },
            inactiveModeAvailability = editState.inactiveModeAvailability,
            inactiveModeUnavailableMessage =
                stringResource(
                    R.string.simulation_mode_temporarily_unavailable,
                    stringResource(
                        if (loadedRoute?.mode == RoutePlaybackMode.GUIDED) {
                            R.string.simulation_mode_name_free_roam
                        } else {
                            R.string.simulation_mode_name_guided
                        },
                    ),
                ),
            onReorder = { from, to -> onEditAction(WaypointEditAction.OnReorder(from, to)) },
            onDelete = { id -> onEditAction(WaypointEditAction.OnDelete(id)) },
            hasUnappliedReorder = editState.hasUnappliedReorder,
            applyChangesLabel = stringResource(R.string.simulation_apply_waypoint_changes_button),
            cancelChangesLabel = stringResource(R.string.simulation_cancel_waypoint_changes_button),
            onApplyChanges = { onEditAction(WaypointEditAction.OnApplyChanges) },
            onCancelChanges = { onEditAction(WaypointEditAction.OnCancelChanges) },
            onDismiss = { onEditAction(WaypointEditAction.OnClose) },
        )
    }

    val pendingAddLatitude = editState.pendingAddLatitude
    val pendingAddLongitude = editState.pendingAddLongitude
    if (pendingAddLatitude != null && pendingAddLongitude != null) {
        AddWaypointConfirmationSheet(
            title = stringResource(R.string.simulation_add_waypoint_title),
            coordinatesMessage = stringResource(R.string.simulation_add_waypoint_message, pendingAddLatitude, pendingAddLongitude),
            saveAsFavoriteLabel = stringResource(R.string.simulation_save_as_favorite_label),
            favoriteNameLabel = stringResource(R.string.simulation_favorite_name_label),
            confirmLabel = stringResource(R.string.simulation_add_waypoint_confirm_button),
            cancelLabel = stringResource(R.string.simulation_add_waypoint_cancel_button),
            isSaveAsFavoriteChecked = editState.isSaveAsFavoriteChecked,
            favoriteNameInput = editState.favoriteNameInput,
            onToggleSaveAsFavorite = { onEditAction(WaypointEditAction.OnToggleSaveAsFavorite) },
            onFavoriteNameChange = { onEditAction(WaypointEditAction.OnFavoriteNameChange(it)) },
            onConfirm = { onEditAction(WaypointEditAction.OnConfirmAdd) },
            onDismiss = { onEditAction(WaypointEditAction.OnDismissAdd) },
        )
    }

    editState.missingRegionsWarning?.let { summary ->
        MissingRegionsWarningSheet(
            summary = summary,
            title = stringResource(R.string.simulation_missing_regions_title),
            message = stringResource(R.string.simulation_missing_regions_message, summary.totalMissingBytes / 1_000_000),
            uncoveredNote = null,
            goToDownloadsLabel = stringResource(R.string.simulation_missing_regions_go_to_downloads),
            continueAnywayLabel = stringResource(R.string.simulation_missing_regions_continue_anyway),
            onGoToDownloads = { onEditAction(WaypointEditAction.OnGoToDownloads) },
            onContinueAnyway = { onEditAction(WaypointEditAction.OnContinueAnyway) },
            onDismiss = { onEditAction(WaypointEditAction.OnDismissMissingRegions) },
        )
    }
}
