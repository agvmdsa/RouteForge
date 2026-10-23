package com.routeforge.routing.presentation.routerequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.FavoriteRoutesRepository
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.coredomain.holder.LastKnownRealLocationHolder
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.usecase.ObserveMapStyleUseCase
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.usecase.ExportRouteFileUseCase
import com.routeforge.routing.domain.usecase.ImportRouteFileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val MIN_WAYPOINTS_TO_PLAY = 2

internal class RouteRequestViewModel(
    private val routeModePreviewer: RouteModePreviewer,
    private val routeComputer: RouteComputer,
    private val routeDraftEditor: RouteDraftEditor,
    private val pendingWaypointConfirmer: PendingWaypointConfirmer,
    private val incomingRouteReceiver: IncomingRouteReceiver,
    private val importRouteFile: ImportRouteFileUseCase,
    private val exportRouteFile: ExportRouteFileUseCase,
    lastKnownRealLocationHolder: LastKnownRealLocationHolder,
    private val draftWaypointsHolder: DraftWaypointsHolder,
    private val favoriteRoutesRepository: FavoriteRoutesRepository,
    observeMapStyle: ObserveMapStyleUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(RouteRequestState(lastKnownLocation = lastKnownRealLocationHolder.location.value))
    val state = _state.asStateFlow()

    private val _events = Channel<RouteRequestEvent>()
    val events = _events.receiveAsFlow()

    init {
        observeMapStyle()
            .onEach { mapStyle -> _state.update { it.copy(mapStyle = mapStyle) } }
            .launchIn(viewModelScope)

        routeDraftEditor.startObservingSelectedFavoriteWaypoint(_state, viewModelScope)

        pendingWaypointConfirmer.startObservingPendingSearchWaypoint(_state, viewModelScope)

        incomingRouteReceiver.startObservingIncomingRoute(_state, viewModelScope)
    }

    fun onAction(action: RouteRequestAction) {
        when (action) {
            is RouteRequestAction.OnMapTap ->
                _state.update { it.copy(pendingAddLatitude = action.latitude, pendingAddLongitude = action.longitude) }
            is RouteRequestAction.OnMarkerDragged ->
                routeDraftEditor.mutateDraft(_state, viewModelScope) {
                    it.update(action.index, RoutePoint(latitude = action.latitude, longitude = action.longitude))
                }
            is RouteRequestAction.OnMarkerClick -> routeDraftEditor.openEditDialog(action.index, _state)
            is RouteRequestAction.OnEditLatitudeChange -> _state.update { it.copy(editLatitudeInput = action.value) }
            is RouteRequestAction.OnEditLongitudeChange -> _state.update { it.copy(editLongitudeInput = action.value) }
            RouteRequestAction.OnConfirmEdit -> routeDraftEditor.confirmEdit(_state, viewModelScope)
            RouteRequestAction.OnDeleteEditingWaypoint -> routeDraftEditor.deleteEditingWaypoint(_state, viewModelScope)
            RouteRequestAction.OnDismissEdit -> _state.update { it.copy(editingIndex = null) }
            RouteRequestAction.OnUndo -> routeDraftEditor.mutateDraft(_state, viewModelScope) { it.undo() }
            RouteRequestAction.OnConfirmAddWaypoint -> pendingWaypointConfirmer.confirmAddWaypoint(_state, viewModelScope)
            RouteRequestAction.OnDismissAddWaypoint -> pendingWaypointConfirmer.dismissAddWaypoint(_state)
            RouteRequestAction.OnToggleSaveAsFavorite ->
                _state.update { it.copy(isSaveAsFavoriteChecked = !it.isSaveAsFavoriteChecked) }
            is RouteRequestAction.OnFavoriteNameInputChange -> _state.update { it.copy(favoriteNameInput = action.value) }
            is RouteRequestAction.OnRouteFileImported -> importRoute(action.bytes, action.format)
            RouteRequestAction.OnConfirmReplaceDraft -> incomingRouteReceiver.confirmReplaceDraft(_state, viewModelScope)
            RouteRequestAction.OnKeepDraft -> _state.update { it.copy(pendingReplaceDraft = null) }
            is RouteRequestAction.OnExportRoute -> exportRoute(action.format)
            RouteRequestAction.OnRequestRoute -> routeComputer.requestRoute(_state, viewModelScope)
            RouteRequestAction.OnUseRoute -> useRoute()
            RouteRequestAction.OnOpenRegionCatalog -> {
                _state.update { it.copy(missingRegionsWarning = null, pendingModePreviewTarget = null) }
                viewModelScope.launch { _events.send(RouteRequestEvent.NavigateToRegionCatalog) }
            }
            RouteRequestAction.OnOpenFavoritesClick ->
                viewModelScope.launch { _events.send(RouteRequestEvent.NavigateToFavorites) }
            RouteRequestAction.OnProceedDespiteMissingRegions -> {
                val previewTarget = _state.value.pendingModePreviewTarget
                val hasOfficialRoute = _state.value.route != null
                _state.update { it.copy(missingRegionsWarning = null, pendingModePreviewTarget = null) }
                when {
                    previewTarget != null && hasOfficialRoute ->
                        routeModePreviewer.attemptModePreview(previewTarget, _state, viewModelScope)
                    previewTarget != null -> routeModePreviewer.computeDraftPreview(_state.value.draft.points, _state, viewModelScope)
                    else -> routeComputer.proceedWithRouteComputation(_state.value.draft.points, _state, viewModelScope)
                }
            }
            RouteRequestAction.OnDismissMissingRegionsWarning ->
                _state.update { it.copy(missingRegionsWarning = null, pendingModePreviewTarget = null) }
            RouteRequestAction.OnSaveRouteClick -> _state.update { it.copy(isSaveRouteSheetOpen = true) }
            is RouteRequestAction.OnRouteNameInputChange -> _state.update { it.copy(routeNameInput = action.value) }
            RouteRequestAction.OnConfirmSaveRoute -> confirmSaveRoute()
            RouteRequestAction.OnDismissSaveRoute -> dismissSaveRoute()
            RouteRequestAction.OnSwitchRouteModeClick -> routeModePreviewer.onSwitchRouteModeClick(_state, viewModelScope)
        }
    }

    private fun confirmSaveRoute() {
        val points = _state.value.draft.points
        val name = _state.value.routeNameInput
        if (points.size < MIN_WAYPOINTS_TO_PLAY || name.isBlank()) return
        favoriteRoutesRepository.add(name, points)
        dismissSaveRoute()
    }

    private fun dismissSaveRoute() {
        _state.update { it.copy(isSaveRouteSheetOpen = false, routeNameInput = "") }
    }

    /** Applies a committed edit from the shared reorder-list sheet (spec 008) via the same
     *  invalidation rule as every other draft mutation (FR-018) — the recompute that already ran
     *  to validate the edit is not reused here; "Finish Route" always recomputes fresh, same as
     *  after any other draft mutation today. */
    fun applyEditedWaypoints(points: List<RoutePoint>) {
        routeDraftEditor.mutateDraft(_state, viewModelScope) { RouteDraft(points = points) }
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

    /** The user explicitly confirms they're done previewing/exporting and wants to play this
     *  route — navigates straight to Simulate with no extra "go to the map?" confirmation; the
     *  mode and route are already fully decided by this point, so that question added nothing.
     *  The route is already in the last-computed-route holder, put there by [RouteComputer]. */
    private fun useRoute() {
        _state.value.route ?: return
        viewModelScope.launch { _events.send(RouteRequestEvent.GoToSimulate) }
    }
}
