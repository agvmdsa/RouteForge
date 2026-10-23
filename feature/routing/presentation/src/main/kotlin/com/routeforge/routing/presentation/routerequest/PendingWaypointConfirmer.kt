package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.FavoriteWaypointsRepository
import com.routeforge.coredomain.holder.PendingSearchWaypointHolder
import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class PendingWaypointConfirmer(
    private val favoriteWaypointsRepository: FavoriteWaypointsRepository,
    private val pendingSearchWaypointHolder: PendingSearchWaypointHolder,
    private val routeDraftEditor: RouteDraftEditor,
) {
    internal fun confirmAddWaypoint(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        val latitude = state.value.pendingAddLatitude
        val longitude = state.value.pendingAddLongitude
        if (latitude == null || longitude == null) return
        if (state.value.isSaveAsFavoriteChecked && state.value.favoriteNameInput.isBlank()) return
        routeDraftEditor.mutateDraft(state, scope) { it.add(RoutePoint(latitude = latitude, longitude = longitude)) }
        if (state.value.isSaveAsFavoriteChecked) {
            favoriteWaypointsRepository.add(state.value.favoriteNameInput, latitude, longitude)
        }
        dismissAddWaypoint(state)
    }

    internal fun dismissAddWaypoint(state: MutableStateFlow<RouteRequestState>) {
        state.update {
            it.copy(
                pendingAddLatitude = null,
                pendingAddLongitude = null,
                pendingAddPlaceName = null,
                isSaveAsFavoriteChecked = false,
                favoriteNameInput = "",
            )
        }
    }

    internal fun startObservingPendingSearchWaypoint(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        pendingSearchWaypointHolder.selected
            .onEach { result ->
                if (result != null) {
                    state.update {
                        it.copy(
                            pendingAddLatitude = result.latitude,
                            pendingAddLongitude = result.longitude,
                            pendingAddPlaceName = result.name.takeIf { it.isNotBlank() },
                        )
                    }
                    pendingSearchWaypointHolder.clear()
                }
            }.launchIn(scope)
    }
}
