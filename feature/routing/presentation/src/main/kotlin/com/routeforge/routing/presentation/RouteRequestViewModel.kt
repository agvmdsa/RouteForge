package com.routeforge.routing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.DraftWaypointsHolder
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.coredomain.LastKnownRealLocationHolder
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFailure
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.usecase.AddWaypointUseCase
import com.routeforge.routing.domain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.routing.domain.usecase.DeleteWaypointUseCase
import com.routeforge.routing.domain.usecase.EditWaypointUseCase
import com.routeforge.routing.domain.usecase.ExportRouteFileUseCase
import com.routeforge.routing.domain.usecase.ImportRouteFileUseCase
import com.routeforge.routing.domain.usecase.MoveWaypointUseCase
import com.routeforge.routing.domain.usecase.PrepareRouteOptionsUseCase
import com.routeforge.routing.domain.usecase.UndoRouteDraftUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val MIN_WAYPOINTS_TO_PLAY = 2

class RouteRequestViewModel(
    private val prepareRouteOptions: PrepareRouteOptionsUseCase,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
    private val importRouteFile: ImportRouteFileUseCase,
    private val exportRouteFile: ExportRouteFileUseCase,
    lastKnownRealLocationHolder: LastKnownRealLocationHolder,
    private val computeRequiredRegions: ComputeRequiredRegionsUseCase,
    private val draftWaypointsHolder: DraftWaypointsHolder,
    private val addWaypoint: AddWaypointUseCase = AddWaypointUseCase(),
    private val moveWaypoint: MoveWaypointUseCase = MoveWaypointUseCase(),
    private val editWaypoint: EditWaypointUseCase = EditWaypointUseCase(),
    private val deleteWaypoint: DeleteWaypointUseCase = DeleteWaypointUseCase(),
    private val undoRouteDraft: UndoRouteDraftUseCase = UndoRouteDraftUseCase(),
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _state = MutableStateFlow(RouteRequestState(lastKnownLocation = lastKnownRealLocationHolder.location.value))
    val state = _state.asStateFlow()

    private val _events = Channel<RouteRequestEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: RouteRequestAction) {
        when (action) {
            is RouteRequestAction.OnMapTap ->
                mutateDraft { addWaypoint(it, RoutePoint(latitude = action.latitude, longitude = action.longitude)) }
            is RouteRequestAction.OnMarkerDragged ->
                mutateDraft {
                    moveWaypoint(it, action.index, RoutePoint(latitude = action.latitude, longitude = action.longitude))
                }
            is RouteRequestAction.OnMarkerClick -> openEditDialog(action.index)
            is RouteRequestAction.OnEditLatitudeChange -> _state.update { it.copy(editLatitudeInput = action.value) }
            is RouteRequestAction.OnEditLongitudeChange -> _state.update { it.copy(editLongitudeInput = action.value) }
            RouteRequestAction.OnConfirmEdit -> confirmEdit()
            RouteRequestAction.OnDeleteEditingWaypoint -> deleteEditingWaypoint()
            RouteRequestAction.OnDismissEdit -> _state.update { it.copy(editingIndex = null) }
            RouteRequestAction.OnUndo -> mutateDraft { undoRouteDraft(it) }
            is RouteRequestAction.OnRouteFileImported -> importRoute(action.bytes, action.format)
            is RouteRequestAction.OnExportRoute -> exportRoute(action.format)
            RouteRequestAction.OnRequestRoute -> requestRoute()
            is RouteRequestAction.OnChooseMode -> chooseMode(action.mode)
            RouteRequestAction.OnUseRoute -> useRoute()
            RouteRequestAction.OnOpenRegionCatalog ->
                viewModelScope.launch { _events.send(RouteRequestEvent.NavigateToRegionCatalog) }
            RouteRequestAction.OnProceedDespiteMissingRegions -> {
                _state.update { it.copy(missingRegionsWarning = null) }
                proceedWithRouteComputation(_state.value.draft.points)
            }
            RouteRequestAction.OnDismissMissingRegionsWarning -> _state.update { it.copy(missingRegionsWarning = null) }
        }
    }

    /** Any draft mutation invalidates a previously computed/chosen route (FR-004's spirit). */
    private fun mutateDraft(transform: (RouteDraft) -> RouteDraft) {
        _state.update {
            it.copy(
                draft = transform(it.draft),
                routeOptions = null,
                chosenMode = null,
                route = null,
                errorType = null,
            )
        }
        draftWaypointsHolder.set(_state.value.draft.points)
    }

    private fun openEditDialog(index: Int) {
        val point = _state.value.draft.points.getOrNull(index) ?: return
        _state.update {
            it.copy(
                editingIndex = index,
                editLatitudeInput = point.latitude.toString(),
                editLongitudeInput = point.longitude.toString(),
            )
        }
    }

    private fun confirmEdit() {
        val index = _state.value.editingIndex ?: return
        val latitude = _state.value.editLatitudeInput.toDoubleOrNull()
        val longitude = _state.value.editLongitudeInput.toDoubleOrNull()
        if (latitude == null || longitude == null) {
            _state.update { it.copy(errorType = RouteRequestError.INVALID_EDIT_COORDINATES) }
            return
        }
        mutateDraft { editWaypoint(it, index, RoutePoint(latitude = latitude, longitude = longitude)) }
        _state.update { it.copy(editingIndex = null) }
    }

    private fun deleteEditingWaypoint() {
        val index = _state.value.editingIndex ?: return
        mutateDraft { deleteWaypoint(it, index) }
        _state.update { it.copy(editingIndex = null) }
    }

    private fun importRoute(
        bytes: ByteArray,
        format: RouteFileFormat,
    ) {
        when (val result = importRouteFile(bytes, format)) {
            is Result.Success -> {
                _state.update {
                    it.copy(
                        draft = result.data,
                        routeOptions = null,
                        chosenMode = null,
                        route = null,
                        errorType = null,
                    )
                }
                draftWaypointsHolder.set(result.data.points)
            }
            is Result.Error -> _state.update { it.copy(errorType = result.error.toRouteRequestError()) }
        }
    }

    private fun exportRoute(format: RouteFileFormat) {
        val route = _state.value.route ?: return
        val bytes = exportRouteFile(route, format)
        viewModelScope.launch { _events.send(RouteRequestEvent.ExportReady(bytes, format)) }
    }

    /** FR + region-awareness: before computing, checks whether the map data Guided mode needs is
     *  actually downloaded, so the user learns about missing regions up front instead of Guided
     *  silently failing to compute later. */
    private fun requestRoute() {
        val points = _state.value.draft.points
        if (points.size < MIN_WAYPOINTS_TO_PLAY) {
            _state.update { it.copy(errorType = RouteRequestError.TOO_FEW_WAYPOINTS_TO_PLAY) }
            return
        }

        val requiredRegions = computeRequiredRegions(points)
        if (!requiredRegions.isFullyDownloaded) {
            _state.update { it.copy(missingRegionsWarning = requiredRegions) }
            return
        }
        proceedWithRouteComputation(points)
    }

    private fun proceedWithRouteComputation(points: List<RoutePoint>) {
        // FR-004: a fresh evaluation always clears any previously locked mode/choice.
        _state.update {
            it.copy(isComputing = true, errorType = null, routeOptions = null, chosenMode = null, route = null)
        }
        viewModelScope.launch {
            val options = withContext(backgroundDispatcher) { prepareRouteOptions(points) }
            if (options.guided == null) {
                // FR-003: Guided isn't computable — skip straight to Free-roam, no choice shown.
                finalizeRoute(options.freeRoam, RoutePlaybackMode.FREE_ROAM)
            } else {
                // FR-002: both modes are viable — let the user choose.
                _state.update { it.copy(isComputing = false, routeOptions = options) }
            }
        }
    }

    private fun chooseMode(mode: RoutePlaybackMode) {
        val options = _state.value.routeOptions ?: return
        val chosen =
            when (mode) {
                RoutePlaybackMode.GUIDED -> options.guided ?: return
                RoutePlaybackMode.FREE_ROAM -> options.freeRoam
            }
        finalizeRoute(chosen, mode)
    }

    private fun finalizeRoute(
        route: Route,
        mode: RoutePlaybackMode,
    ) {
        _state.update { it.copy(isComputing = false, routeOptions = null, chosenMode = mode, route = route) }
        lastComputedRouteHolder.set(route)
    }

    /** The user explicitly confirms they're done previewing/exporting and wants to play this route. */
    private fun useRoute() {
        val route = _state.value.route ?: return
        viewModelScope.launch { _events.send(RouteRequestEvent.RouteComputed(route)) }
    }
}

private fun RouteFileFailure.toRouteRequestError(): RouteRequestError =
    when (this) {
        RouteFileFailure.TooFewWaypoints -> RouteRequestError.IMPORT_TOO_FEW_WAYPOINTS
        is RouteFileFailure.CoordinateOutOfRange -> RouteRequestError.IMPORT_COORDINATE_OUT_OF_RANGE
        RouteFileFailure.Malformed -> RouteRequestError.IMPORT_MALFORMED
    }

