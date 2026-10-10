package com.routeforge.simulation.presentation.waypointedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.FavoriteWaypointsRepository
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.usecase.ComputeEditableWaypointRangeUseCase
import com.routeforge.coredomain.usecase.RecomputeRouteForBothModesUseCase
import com.routeforge.coredomain.usecase.RouteRecomputeResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_EDITABLE_WAYPOINTS = 2

/** Simulate's half of spec 008's shared edit-sheet feature (research.md Decision 9) — a second,
 *  sibling ViewModel to [com.routeforge.simulation.presentation.SimulationViewModel], owning only
 *  this feature's own sheet/working-list state. Every committed edit (reorder, marker drag,
 *  confirmed map-tap add, delete) cancels any in-flight recompute (FR-017) and re-runs
 *  [RecomputeRouteForBothModesUseCase]; a successful result is reported via [events] for
 *  [com.routeforge.simulation.presentation.SimulationViewModel] to apply to the live session. */
class WaypointEditViewModel(
    private val recomputeRouteForBothModes: RecomputeRouteForBothModesUseCase,
    private val computeEditableWaypointRange: ComputeEditableWaypointRangeUseCase,
    private val favoriteWaypointsRepository: FavoriteWaypointsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WaypointEditState())
    val state = _state.asStateFlow()

    private val _events = Channel<WaypointEditEvent>()
    val events = _events.receiveAsFlow()

    private var route: Route? = null
    private var distanceTraveledMeters: Double? = null
    private var editJob: Job? = null

    /** The last point list a recompute actually succeeded for — what a reorder reverts to if the
     *  eventual [OnApplyChanges] (or a different edit committed before it) fails. */
    private var committedPoints: List<RoutePoint> = emptyList()

    fun onAction(action: WaypointEditAction) {
        when (action) {
            is WaypointEditAction.OnOpen -> open(action.route, action.distanceTraveledMeters)
            WaypointEditAction.OnClose -> close()
            is WaypointEditAction.OnMapTap ->
                _state.update { it.copy(pendingAddLatitude = action.latitude, pendingAddLongitude = action.longitude) }
            WaypointEditAction.OnDismissAdd -> clearPendingAdd()
            WaypointEditAction.OnToggleSaveAsFavorite ->
                _state.update { it.copy(isSaveAsFavoriteChecked = !it.isSaveAsFavoriteChecked) }
            is WaypointEditAction.OnFavoriteNameChange -> _state.update { it.copy(favoriteNameInput = action.value) }
            WaypointEditAction.OnConfirmAdd -> confirmAdd()
            is WaypointEditAction.OnMarkerDragged -> onMarkerDragged(action.index, action.latitude, action.longitude)
            is WaypointEditAction.OnReorder -> onReorder(action.fromIndex, action.toIndex)
            WaypointEditAction.OnApplyChanges -> onApplyChanges()
            WaypointEditAction.OnCancelChanges ->
                _state.update { it.copy(points = committedPoints, hasUnappliedReorder = false) }
            is WaypointEditAction.OnDelete -> onDelete(action.id)
            WaypointEditAction.OnDismissMissingRegions ->
                _state.update { it.copy(missingRegionsWarning = null, pendingEditPoints = null) }
            WaypointEditAction.OnGoToDownloads -> onGoToDownloads()
            WaypointEditAction.OnContinueAnyway -> onContinueAnyway()
        }
    }

    private fun open(
        route: Route,
        distanceTraveledMeters: Double?,
    ) {
        this.route = route
        this.distanceTraveledMeters = distanceTraveledMeters
        this.committedPoints = route.points
        val range = computeEditableWaypointRange(route, distanceTraveledMeters)
        _state.value =
            WaypointEditState(
                isOpen = true,
                isSessionActive = distanceTraveledMeters != null,
                points = route.points,
                firstEditableIndex = range.firstEditableIndex,
            )
    }

    private fun close() {
        editJob?.cancel()
        editJob = null
        route = null
        distanceTraveledMeters = null
        _state.value = WaypointEditState()
    }

    private fun clearPendingAdd() {
        _state.update {
            it.copy(
                pendingAddLatitude = null,
                pendingAddLongitude = null,
                isSaveAsFavoriteChecked = false,
                favoriteNameInput = "",
            )
        }
    }

    private fun confirmAdd() {
        val latitude = _state.value.pendingAddLatitude ?: return
        val longitude = _state.value.pendingAddLongitude ?: return
        val isSaveAsFavoriteChecked = _state.value.isSaveAsFavoriteChecked
        val favoriteNameInput = _state.value.favoriteNameInput
        clearPendingAdd()
        if (isSaveAsFavoriteChecked && favoriteNameInput.isNotBlank()) {
            favoriteWaypointsRepository.add(favoriteNameInput, latitude, longitude)
        }
        commitEdit(_state.value.points + RoutePoint(latitude = latitude, longitude = longitude))
    }

    private fun onMarkerDragged(
        index: Int,
        latitude: Double,
        longitude: Double,
    ) {
        val current = _state.value
        if (index < current.firstEditableIndex) return
        val newPoints = current.points.toMutableList()
        newPoints[index] = RoutePoint(latitude = latitude, longitude = longitude)
        commitEdit(newPoints)
    }

    /** Reordering only updates the displayed order and marks it unapplied — recomputing on every
     *  intermediate swap during a drag made the gesture janky, so the actual recompute is deferred
     *  to an explicit [onApplyChanges] (or whatever other edit commits next, which naturally carries
     *  this reorder along with it since it always operates on the current working [points]). */
    private fun onReorder(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val current = _state.value
        if (fromIndex < current.firstEditableIndex || toIndex < current.firstEditableIndex) return
        val newPoints = current.points.toMutableList()
        newPoints.add(toIndex, newPoints.removeAt(fromIndex))
        _state.update { it.copy(points = newPoints, hasUnappliedReorder = true) }
    }

    private fun onApplyChanges() {
        if (!_state.value.hasUnappliedReorder) return
        commitEdit(_state.value.points, closeOnSuccess = true)
    }

    private fun onDelete(id: String) {
        val current = _state.value
        val index = id.removePrefix("wp").toIntOrNull() ?: return
        if (index < current.firstEditableIndex) return
        val editableCount = current.points.size - current.firstEditableIndex
        if (editableCount <= MIN_EDITABLE_WAYPOINTS) return
        val newPoints = current.points.toMutableList()
        newPoints.removeAt(index)
        commitEdit(newPoints)
    }

    private fun onGoToDownloads() {
        val wasSessionActive = _state.value.isSessionActive
        _state.update { it.copy(missingRegionsWarning = null, pendingEditPoints = null) }
        viewModelScope.launch {
            _events.send(WaypointEditEvent.GoToDownloads)
            if (wasSessionActive) _events.send(WaypointEditEvent.AutoPauseRequested)
        }
    }

    private fun onContinueAnyway() {
        val pending = _state.value.pendingEditPoints
        _state.update { it.copy(missingRegionsWarning = null, pendingEditPoints = null) }
        if (pending != null) commitEdit(pending, bypassRegionCheck = true)
    }

    private fun commitEdit(
        newPoints: List<RoutePoint>,
        bypassRegionCheck: Boolean = false,
        closeOnSuccess: Boolean = false,
    ) {
        val activeMode = route?.mode ?: return
        editJob?.cancel()
        editJob =
            viewModelScope.launch {
                when (val result = recomputeRouteForBothModes(newPoints, activeMode, bypassRegionCheck)) {
                    is RouteRecomputeResult.Applied -> {
                        route = result.activeRoute
                        committedPoints = newPoints
                        val range = computeEditableWaypointRange(result.activeRoute, distanceTraveledMeters)
                        _state.update {
                            it.copy(
                                points = newPoints,
                                hasUnappliedReorder = false,
                                firstEditableIndex = range.firstEditableIndex,
                                inactiveModeAvailability = result.inactiveModeAvailability,
                                errorMessage = null,
                                isOpen = if (closeOnSuccess) false else it.isOpen,
                            )
                        }
                        _events.send(WaypointEditEvent.Committed(newPoints, result.activeRoute))
                        if (closeOnSuccess) _events.send(WaypointEditEvent.WaypointsUpdated)
                    }
                    is RouteRecomputeResult.ActiveModeFailed ->
                        _state.update { it.copy(points = committedPoints, hasUnappliedReorder = false, errorMessage = result.reason) }
                    is RouteRecomputeResult.MissingRegions ->
                        _state.update { it.copy(missingRegionsWarning = result.summary, pendingEditPoints = newPoints) }
                }
            }
    }
}
