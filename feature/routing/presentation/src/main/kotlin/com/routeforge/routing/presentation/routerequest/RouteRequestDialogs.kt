package com.routeforge.routing.presentation.routerequest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.routeforge.designsystem.components.TwoButtonConfirmationSheet
import com.routeforge.designsystem.sheets.AddWaypointConfirmationSheet
import com.routeforge.designsystem.sheets.MissingRegionsWarningSheet
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.routing.presentation.R

/** The screen's modal sheets — each gated on its own nullable/boolean state field, shown at most
 *  one at a time in practice since they correspond to mutually-exclusive user flows. */
@Composable
internal fun RouteRequestDialogs(
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

    // A route that arrived from another app while waypoints were placed; leaving the sheet keeps them.
    if (state.pendingReplaceDraft != null) {
        TwoButtonConfirmationSheet(
            title = stringResource(R.string.routing_replace_draft_title),
            message = stringResource(R.string.routing_replace_draft_message),
            confirmLabel = stringResource(R.string.routing_replace_draft_confirm),
            dismissLabel = stringResource(R.string.routing_replace_draft_keep),
            onConfirm = { onAction(RouteRequestAction.OnConfirmReplaceDraft) },
            onDismiss = { onAction(RouteRequestAction.OnKeepDraft) },
        )
    }
}

@Preview
@Composable
private fun RouteRequestDialogsPreview() {
    RouteForgeTheme {
        RouteRequestDialogs(state = RouteRequestState(isSaveRouteSheetOpen = true), onAction = {})
    }
}
