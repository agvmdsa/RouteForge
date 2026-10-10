package com.routeforge.routing.presentation.routerequest.waypointedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.usecase.RecomputeRouteForBothModesUseCase
import com.routeforge.coredomain.usecase.RouteRecomputeResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_WAYPOINTS = 2

/** Plan Route's half of spec 008's shared edit-sheet feature (research.md Decision 9) — a second,
 *  sibling ViewModel to [com.routeforge.routing.presentation.routerequest.RouteRequestViewModel].
 *  Only the reorder-list's own reorder/delete actions go through this pipeline; Plan Route's
 *  existing map-tap-add/marker-drag-move paths are unchanged. Per research.md Decision 12
 *  (`/speckit-analyze` finding C3), [WaypointEditAction.OnOpen.activeMode] is Free-roam whenever
 *  no mode has been chosen yet on Plan Route — it always succeeds, so a pre-choice edit here can
 *  never trigger FR-010's revert path, matching Plan Route's existing never-reverts behavior. */
class WaypointEditViewModel(
    private val recomputeRouteForBothModes: RecomputeRouteForBothModesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(WaypointEditState())
    val state = _state.asStateFlow()

    private val _events = Channel<WaypointEditEvent>()
    val events = _events.receiveAsFlow()

    private var activeMode: RoutePlaybackMode = RoutePlaybackMode.FREE_ROAM
    private var editJob: Job? = null

    /** The last point list a recompute actually succeeded for — what a reorder reverts to if the
     *  eventual [WaypointEditAction.OnApplyChanges] (or a different edit committed before it) fails. */
    private var committedPoints: List<RoutePoint> = emptyList()

    fun onAction(action: WaypointEditAction) {
        when (action) {
            is WaypointEditAction.OnOpen -> {
                activeMode = action.activeMode
                committedPoints = action.points
                _state.value = WaypointEditState(isOpen = true, points = action.points)
            }
            WaypointEditAction.OnClose -> {
                editJob?.cancel()
                editJob = null
                _state.value = WaypointEditState()
            }
            is WaypointEditAction.OnReorder -> {
                val points = _state.value.points.toMutableList()
                points.add(action.toIndex, points.removeAt(action.fromIndex))
                _state.update { it.copy(points = points, hasUnappliedReorder = true) }
            }
            WaypointEditAction.OnApplyChanges -> {
                if (_state.value.hasUnappliedReorder) commitEdit(_state.value.points, closeOnSuccess = true)
            }
            WaypointEditAction.OnCancelChanges ->
                _state.update { it.copy(points = committedPoints, hasUnappliedReorder = false) }
            is WaypointEditAction.OnDelete -> {
                val current = _state.value
                if (current.points.size <= MIN_WAYPOINTS) return
                val index = action.id.removePrefix("wp").toIntOrNull() ?: return
                val points = current.points.toMutableList()
                points.removeAt(index)
                commitEdit(points)
            }
            WaypointEditAction.OnDismissMissingRegions ->
                _state.update { it.copy(missingRegionsWarning = null, pendingEditPoints = null) }
            WaypointEditAction.OnGoToDownloads -> {
                _state.update { it.copy(missingRegionsWarning = null, pendingEditPoints = null) }
                viewModelScope.launch { _events.send(WaypointEditEvent.GoToDownloads) }
            }
            WaypointEditAction.OnContinueAnyway -> {
                val pending = _state.value.pendingEditPoints
                _state.update { it.copy(missingRegionsWarning = null, pendingEditPoints = null) }
                if (pending != null) commitEdit(pending, bypassRegionCheck = true)
            }
        }
    }

    private fun commitEdit(
        points: List<RoutePoint>,
        bypassRegionCheck: Boolean = false,
        closeOnSuccess: Boolean = false,
    ) {
        editJob?.cancel()
        editJob =
            viewModelScope.launch {
                when (val result = recomputeRouteForBothModes(points, activeMode, bypassRegionCheck)) {
                    is RouteRecomputeResult.Applied -> {
                        committedPoints = points
                        _state.update {
                            it.copy(
                                points = points,
                                hasUnappliedReorder = false,
                                inactiveModeAvailability = result.inactiveModeAvailability,
                                errorMessage = null,
                                isOpen = if (closeOnSuccess) false else it.isOpen,
                            )
                        }
                        _events.send(WaypointEditEvent.Committed(points, result.activeRoute))
                        if (closeOnSuccess) _events.send(WaypointEditEvent.WaypointsUpdated)
                    }
                    is RouteRecomputeResult.ActiveModeFailed ->
                        _state.update { it.copy(points = committedPoints, hasUnappliedReorder = false, errorMessage = result.reason) }
                    is RouteRecomputeResult.MissingRegions ->
                        _state.update { it.copy(missingRegionsWarning = result.summary, pendingEditPoints = points) }
                }
            }
    }
}
