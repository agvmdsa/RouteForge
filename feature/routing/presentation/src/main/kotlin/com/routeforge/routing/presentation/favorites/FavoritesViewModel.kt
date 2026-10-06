package com.routeforge.routing.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.FavoriteWaypointsRepository
import com.routeforge.coredomain.NetworkConnectivityChecker
import com.routeforge.coredomain.holder.PendingTeleportTargetHolder
import com.routeforge.coredomain.holder.SelectedFavoriteWaypointHolder
import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FavoritesViewModel(
    isPickerMode: Boolean,
    private val favoriteWaypointsRepository: FavoriteWaypointsRepository,
    private val pendingTeleportTargetHolder: PendingTeleportTargetHolder,
    private val selectedFavoriteWaypointHolder: SelectedFavoriteWaypointHolder,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
) : ViewModel() {
    private val _state = MutableStateFlow(FavoritesState(isPickerMode = isPickerMode))
    val state = _state.asStateFlow()

    private val _events = Channel<FavoritesEvent>()
    val events = _events.receiveAsFlow()

    init {
        favoriteWaypointsRepository.observeFavorites()
            .onEach { favorites -> _state.update { it.copy(favorites = favorites) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: FavoritesAction) {
        when (action) {
            is FavoritesAction.OnFavoriteSelected -> onFavoriteSelected(action.id)
            is FavoritesAction.OnEditClick -> onEditClick(action.id)
            is FavoritesAction.OnEditNameChange -> _state.update { it.copy(editNameInput = action.value) }
            is FavoritesAction.OnEditLatitudeChange -> _state.update { it.copy(editLatitudeInput = action.value) }
            is FavoritesAction.OnEditLongitudeChange -> _state.update { it.copy(editLongitudeInput = action.value) }
            FavoritesAction.OnConfirmEdit -> confirmEdit()
            FavoritesAction.OnDismissEdit -> dismissEdit()
            is FavoritesAction.OnDeleteClick -> _state.update { it.copy(pendingDeleteId = action.id) }
            FavoritesAction.OnConfirmDelete -> confirmDelete()
            FavoritesAction.OnDismissDelete -> _state.update { it.copy(pendingDeleteId = null) }
            is FavoritesAction.OnMockHereClick -> onMockHereClick(action.id)
            FavoritesAction.OnConfirmMockHere -> confirmMockHere()
            FavoritesAction.OnDismissMockHere -> _state.update { it.copy(pendingTeleportFavorite = null, isPendingTeleportBlockedOffline = false) }
        }
    }

    private fun onEditClick(id: String) {
        val favorite = _state.value.favorites.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                editingId = id,
                editNameInput = favorite.name,
                editLatitudeInput = favorite.latitude.toString(),
                editLongitudeInput = favorite.longitude.toString(),
                editError = null,
            )
        }
    }

    private fun confirmEdit() {
        val id = _state.value.editingId ?: return
        val name = _state.value.editNameInput
        if (name.isBlank()) {
            _state.update { it.copy(editError = FavoritesError.INVALID_NAME) }
            return
        }
        val latitude = _state.value.editLatitudeInput.toDoubleOrNull()
        val longitude = _state.value.editLongitudeInput.toDoubleOrNull()
        if (latitude == null || longitude == null) {
            _state.update { it.copy(editError = FavoritesError.INVALID_COORDINATES) }
            return
        }
        favoriteWaypointsRepository.update(id, name, latitude, longitude)
        dismissEdit()
    }

    private fun dismissEdit() {
        _state.update {
            it.copy(editingId = null, editNameInput = "", editLatitudeInput = "", editLongitudeInput = "", editError = null)
        }
    }

    private fun confirmDelete() {
        val id = _state.value.pendingDeleteId ?: return
        favoriteWaypointsRepository.delete(id)
        _state.update { it.copy(pendingDeleteId = null) }
    }

    private fun onFavoriteSelected(id: String) {
        if (!_state.value.isPickerMode) return
        val favorite = _state.value.favorites.firstOrNull { it.id == id } ?: return
        selectedFavoriteWaypointHolder.set(RoutePoint(latitude = favorite.latitude, longitude = favorite.longitude))
        viewModelScope.launch { _events.send(FavoritesEvent.NavigateBack) }
    }

    private fun onMockHereClick(id: String) {
        val favorite = _state.value.favorites.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(pendingTeleportFavorite = favorite, isPendingTeleportBlockedOffline = !networkConnectivityChecker.isConnected())
        }
    }

    private fun confirmMockHere() {
        val favorite = _state.value.pendingTeleportFavorite ?: return
        if (_state.value.isPendingTeleportBlockedOffline) return
        _state.update { it.copy(pendingTeleportFavorite = null, isPendingTeleportBlockedOffline = false) }
        pendingTeleportTargetHolder.set(RoutePoint(latitude = favorite.latitude, longitude = favorite.longitude))
        viewModelScope.launch { _events.send(FavoritesEvent.NavigateBack) }
    }
}
