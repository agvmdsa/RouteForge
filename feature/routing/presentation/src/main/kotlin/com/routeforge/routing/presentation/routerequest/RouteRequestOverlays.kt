package com.routeforge.routing.presentation.routerequest

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.designsystem.components.AutoDismissConfirmationPill
import com.routeforge.designsystem.components.FabColumn
import com.routeforge.designsystem.components.TopBanner
import com.routeforge.designsystem.components.tourTarget
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.designsystem.theme.Spacing
import com.routeforge.routing.presentation.R
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditAction
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditState

/** Status pill, error banner, and the transient "waypoints updated" confirmation — the map's
 *  top-anchored overlays, as opposed to the bottom-anchored FAB columns. */
@Composable
internal fun BoxScope.RouteRequestOverlays(
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
internal fun BoxScope.RouteRequestLeadingFabs(
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
internal fun BoxScope.RouteRequestTrailingFabs(
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
                            imageVector =
                                if (state.previewMode ==
                                    RoutePlaybackMode.GUIDED
                                ) {
                                    Icons.Filled.Straighten
                                } else {
                                    Icons.Filled.Signpost
                                },
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
private fun RouteRequestOverlaysPreview() {
    RouteForgeTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            RouteRequestOverlays(
                state = RouteRequestState(errorType = RouteRequestError.TOO_FEW_WAYPOINTS_TO_PLAY),
                showWaypointsUpdatedConfirmation = false,
                onDismissWaypointsUpdatedConfirmation = {},
            )
        }
    }
}

@Preview
@Composable
private fun RouteRequestLeadingFabsPreview() {
    RouteForgeTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            RouteRequestLeadingFabs(
                state = RouteRequestState(),
                onAction = {},
                onImportClick = {},
                editState = WaypointEditState(),
                onEditAction = {},
            )
        }
    }
}

@Preview
@Composable
private fun RouteRequestTrailingFabsPreview() {
    RouteForgeTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            RouteRequestTrailingFabs(state = RouteRequestState(), onAction = {})
        }
    }
}
