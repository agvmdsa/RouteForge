package com.routeforge.routing.presentation.routerequest

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.routeforge.coredomain.model.ModeAvailability
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.designsystem.sheets.MissingRegionsWarningSheet
import com.routeforge.designsystem.waypointedit.WaypointEditSheet
import com.routeforge.designsystem.waypointedit.WaypointListItem
import com.routeforge.routing.presentation.R
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditAction
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditState

/** An ordinary button (no FAB-toggle/mode-gating, FR-005 — Plan Route's tap-to-add is already its
 *  only tap meaning) opening the shared reorder-list sheet. Plan Route's existing map-tap-add/
 *  marker-drag-move paths are untouched by this feature. */
@Composable
fun WaypointEditSection(
    points: List<RoutePoint>,
    chosenMode: RoutePlaybackMode?,
    editState: WaypointEditState,
    onEditAction: (WaypointEditAction) -> Unit,
) {
    if (!editState.isOpen && points.size >= 2) {
        FloatingActionButton(
            onClick = { onEditAction(WaypointEditAction.OnOpen(points, chosenMode ?: RoutePlaybackMode.FREE_ROAM)) },
        ) {
            Icon(Icons.Filled.Reorder, contentDescription = stringResource(R.string.routing_edit_waypoints_button))
        }
    }

    if (editState.isOpen) {
        WaypointEditSheet(
            title = stringResource(R.string.routing_waypoint_edit_sheet_title),
            items =
                editState.points.mapIndexed { index, point ->
                    WaypointListItem(
                        id = "wp$index",
                        label = "${index + 1}. ${"%.5f".format(point.latitude)}, ${"%.5f".format(point.longitude)}",
                        isEditable = true,
                    )
                },
            inactiveModeAvailability = editState.inactiveModeAvailability,
            inactiveModeUnavailableMessage =
                stringResource(
                    R.string.routing_mode_temporarily_unavailable,
                    if (chosenMode == RoutePlaybackMode.GUIDED) "Free-roam" else "Guided",
                ),
            onReorder = { from, to -> onEditAction(WaypointEditAction.OnReorder(from, to)) },
            onDelete = { id -> onEditAction(WaypointEditAction.OnDelete(id)) },
            hasUnappliedReorder = editState.hasUnappliedReorder,
            applyChangesLabel = stringResource(R.string.routing_apply_waypoint_changes_button),
            cancelChangesLabel = stringResource(R.string.routing_cancel_waypoint_changes_button),
            onApplyChanges = { onEditAction(WaypointEditAction.OnApplyChanges) },
            onCancelChanges = { onEditAction(WaypointEditAction.OnCancelChanges) },
            onDismiss = { onEditAction(WaypointEditAction.OnClose) },
        )
    }

    editState.missingRegionsWarning?.let { summary ->
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
            onGoToDownloads = { onEditAction(WaypointEditAction.OnGoToDownloads) },
            onContinueAnyway = { onEditAction(WaypointEditAction.OnContinueAnyway) },
            onDismiss = { onEditAction(WaypointEditAction.OnDismissMissingRegions) },
        )
    }
}
