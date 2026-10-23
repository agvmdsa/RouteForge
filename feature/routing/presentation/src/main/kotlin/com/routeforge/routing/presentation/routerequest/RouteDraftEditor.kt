package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.coredomain.holder.SelectedFavoriteWaypointHolder
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.model.RouteDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class RouteDraftEditor(
    private val draftWaypointsHolder: DraftWaypointsHolder,
    private val selectedFavoriteWaypointHolder: SelectedFavoriteWaypointHolder,
    private val routeModePreviewer: RouteModePreviewer,
) {
    /** Any draft mutation invalidates a previously computed/chosen route (FR-004's spirit). */
    internal fun mutateDraft(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
        transform: (RouteDraft) -> RouteDraft,
    ) {
        val wasPreviewingGuided = state.value.previewMode == RoutePlaybackMode.GUIDED
        state.update {
            it.copy(
                draft = transform(it.draft),
                chosenMode = null,
                route = null,
                errorType = null,
                previewMode = if (wasPreviewingGuided) RoutePlaybackMode.GUIDED else null,
                // previewRoute is deliberately left as-is here (stale, but still the right mode's
                // shape) rather than cleared to null — clearing it would flash the map to the plain
                // straight-line view for the brief recompute window below, which read as "switched
                // to Free-roam and back." routeModePreviewer.refreshGuidedPreview(state, scope) below replaces it with the fresh
                // result; a non-Guided preview still gets cleared since there's nothing to carry over.
                previewRoute = if (wasPreviewingGuided) it.previewRoute else null,
            )
        }
        draftWaypointsHolder.set(state.value.draft.points)
        // A Guided preview that was already on screen should carry over and recompute with the
        // new point list, rather than silently dropping back to the default free-roam view —
        // the user has to explicitly turn it off (OnSwitchRouteModeClick) to lose it.
        if (wasPreviewingGuided) routeModePreviewer.refreshGuidedPreview(state, scope)
    }

    internal fun openEditDialog(
        index: Int,
        state: MutableStateFlow<RouteRequestState>,
    ) {
        val point =
            state.value.draft.points
                .getOrNull(index) ?: return
        state.update {
            it.copy(
                editingIndex = index,
                editLatitudeInput = point.latitude.toString(),
                editLongitudeInput = point.longitude.toString(),
            )
        }
    }

    internal fun confirmEdit(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        val index = state.value.editingIndex ?: return
        val latitude = state.value.editLatitudeInput.toDoubleOrNull()
        val longitude = state.value.editLongitudeInput.toDoubleOrNull()
        if (latitude == null || longitude == null) {
            state.update { it.copy(errorType = RouteRequestError.INVALID_EDIT_COORDINATES) }
            return
        }
        mutateDraft(state, scope) { it.update(index, RoutePoint(latitude = latitude, longitude = longitude)) }
        state.update { it.copy(editingIndex = null) }
    }

    internal fun deleteEditingWaypoint(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        val index = state.value.editingIndex ?: return
        mutateDraft(state, scope) { it.delete(index) }
        state.update { it.copy(editingIndex = null) }
    }

    internal fun startObservingSelectedFavoriteWaypoint(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        selectedFavoriteWaypointHolder.selected
            .onEach { point ->
                if (point != null) {
                    mutateDraft(state, scope) { it.add(point) }
                    selectedFavoriteWaypointHolder.clear()
                }
            }.launchIn(scope)
    }
}
