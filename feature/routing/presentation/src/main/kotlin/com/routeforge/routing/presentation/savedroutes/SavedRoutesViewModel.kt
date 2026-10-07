package com.routeforge.routing.presentation.savedroutes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.FavoriteRoutesRepository
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.FavoriteRoute
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.model.RouteOptions
import com.routeforge.routing.domain.model.autoResolved
import com.routeforge.routing.domain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.routing.domain.usecase.ExportRouteFileUseCase
import com.routeforge.routing.domain.usecase.PrepareRouteOptionsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedRoutesViewModel(
    private val favoriteRoutesRepository: FavoriteRoutesRepository,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
    private val computeRequiredRegions: ComputeRequiredRegionsUseCase,
    private val prepareRouteOptions: PrepareRouteOptionsUseCase,
    private val exportRouteFile: ExportRouteFileUseCase,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _state = MutableStateFlow(SavedRoutesState())
    val state = _state.asStateFlow()

    private val _events = Channel<SavedRoutesEvent>()
    val events = _events.receiveAsFlow()

    init {
        favoriteRoutesRepository.observeFavoriteRoutes()
            .onEach { routes -> _state.update { it.copy(routes = routes) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: SavedRoutesAction) {
        when (action) {
            is SavedRoutesAction.OnEditClick -> onEditClick(action.id)
            is SavedRoutesAction.OnEditNameChange -> _state.update { it.copy(editNameInput = action.value) }
            SavedRoutesAction.OnConfirmEdit -> confirmEdit()
            SavedRoutesAction.OnDismissEdit -> dismissEdit()
            is SavedRoutesAction.OnDeleteClick -> _state.update { it.copy(pendingDeleteId = action.id) }
            SavedRoutesAction.OnConfirmDelete -> confirmDelete()
            SavedRoutesAction.OnDismissDelete -> _state.update { it.copy(pendingDeleteId = null) }
            is SavedRoutesAction.OnUseClick -> onUseClick(action.id)
            is SavedRoutesAction.OnChooseMode -> onChooseMode(action.mode)
            SavedRoutesAction.OnDismissModeChoice ->
                _state.update { it.copy(routeOptions = null, resolvingRouteId = null, pendingExportFormat = null) }
            SavedRoutesAction.OnOpenRegionCatalog -> {
                _state.update { it.copy(missingRegionsWarning = null, resolvingRouteId = null, pendingExportFormat = null) }
                viewModelScope.launch { _events.send(SavedRoutesEvent.NavigateToRegionCatalog) }
            }
            SavedRoutesAction.OnProceedDespiteMissingRegions -> proceedDespiteMissingRegions()
            SavedRoutesAction.OnDismissMissingRegionsWarning ->
                _state.update { it.copy(missingRegionsWarning = null, resolvingRouteId = null, pendingExportFormat = null) }
            is SavedRoutesAction.OnExportClick -> _state.update { it.copy(pendingExportRouteId = action.id) }
            is SavedRoutesAction.OnChooseExportFormat -> onChooseExportFormat(action.format)
            SavedRoutesAction.OnDismissExportFormat -> _state.update { it.copy(pendingExportRouteId = null) }
        }
    }

    private fun onEditClick(id: String) {
        val route = _state.value.routes.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(editingId = id, editNameInput = route.name, editError = null) }
    }

    private fun confirmEdit() {
        val id = _state.value.editingId ?: return
        val name = _state.value.editNameInput
        if (name.isBlank()) {
            _state.update { it.copy(editError = SavedRoutesError.INVALID_NAME) }
            return
        }
        favoriteRoutesRepository.rename(id, name)
        dismissEdit()
    }

    private fun dismissEdit() {
        _state.update { it.copy(editingId = null, editNameInput = "", editError = null) }
    }

    private fun confirmDelete() {
        val id = _state.value.pendingDeleteId ?: return
        favoriteRoutesRepository.delete(id)
        _state.update { it.copy(pendingDeleteId = null) }
    }

    private fun onUseClick(id: String) {
        val route = _state.value.routes.firstOrNull { it.id == id } ?: return
        val requiredRegions = computeRequiredRegions(route.points)
        if (!requiredRegions.isFullyDownloaded) {
            _state.update { it.copy(missingRegionsWarning = requiredRegions, resolvingRouteId = id) }
            return
        }
        prepareOptionsFor(route)
    }

    private fun proceedDespiteMissingRegions() {
        val route = _state.value.routes.firstOrNull { it.id == _state.value.resolvingRouteId } ?: return
        _state.update { it.copy(missingRegionsWarning = null) }
        prepareOptionsFor(route)
    }

    private fun prepareOptionsFor(route: FavoriteRoute) {
        _state.update { it.copy(isComputingId = route.id, resolvingRouteId = route.id) }
        viewModelScope.launch {
            val options = withContext(backgroundDispatcher) { prepareRouteOptions(route.points) }
            val autoResolved = options.autoResolved
            if (autoResolved != null) {
                resolve(autoResolved.first, autoResolved.second, options)
            } else {
                _state.update { it.copy(isComputingId = null, routeOptions = options) }
            }
        }
    }

    private fun onChooseMode(mode: RoutePlaybackMode) {
        val options = _state.value.routeOptions ?: return
        val chosen =
            when (mode) {
                RoutePlaybackMode.GUIDED -> options.guided ?: return
                RoutePlaybackMode.FREE_ROAM -> options.freeRoam
            }
        resolve(chosen, mode, options)
    }

    /** Shares the exact same region-check + mode-resolution pipeline as [onUseClick] — exporting
     *  still needs a fully computed [Route] with geometry, same prerequisite as playing it.
     *  [resolve] branches on [SavedRoutesState.pendingExportFormat] to decide whether the result
     *  gets exported or handed off to Simulate. */
    private fun onChooseExportFormat(format: RouteFileFormat) {
        val id = _state.value.pendingExportRouteId ?: return
        val route = _state.value.routes.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(pendingExportRouteId = null, pendingExportFormat = format) }
        val requiredRegions = computeRequiredRegions(route.points)
        if (!requiredRegions.isFullyDownloaded) {
            _state.update { it.copy(missingRegionsWarning = requiredRegions, resolvingRouteId = id) }
            return
        }
        prepareOptionsFor(route)
    }

    private fun resolve(
        route: Route,
        mode: RoutePlaybackMode,
        options: RouteOptions,
    ) {
        val withAlternate =
            when (mode) {
                RoutePlaybackMode.GUIDED ->
                    route.copy(alternateGeometry = options.freeRoam.geometry, alternateDistanceMeters = options.freeRoam.distanceMeters)
                RoutePlaybackMode.FREE_ROAM ->
                    route.copy(alternateGeometry = options.guided?.geometry, alternateDistanceMeters = options.guided?.distanceMeters)
            }
        val resolved = withAlternate.copy(mode = mode)
        val exportFormat = _state.value.pendingExportFormat
        _state.update {
            it.copy(isComputingId = null, routeOptions = null, resolvingRouteId = null, pendingExportFormat = null)
        }
        if (exportFormat != null) {
            val bytes = exportRouteFile(resolved, exportFormat)
            viewModelScope.launch { _events.send(SavedRoutesEvent.ExportReady(bytes, exportFormat)) }
        } else {
            lastComputedRouteHolder.set(resolved)
            viewModelScope.launch { _events.send(SavedRoutesEvent.NavigateBack) }
        }
    }
}
