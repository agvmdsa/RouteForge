package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.AddressLookup
import com.routeforge.coredomain.FavoriteWaypointsRepository
import com.routeforge.coredomain.RecentPlacesRepository
import com.routeforge.coredomain.handoff.PendingSearchWaypointHandoffReceiver
import com.routeforge.coredomain.model.NamedPlace
import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class PendingWaypointConfirmer(
    private val favoriteWaypointsRepository: FavoriteWaypointsRepository,
    private val addressLookup: AddressLookup,
    private val recentPlacesRepository: RecentPlacesRepository,
    private val pendingSearchWaypointReceiver: PendingSearchWaypointHandoffReceiver,
    private val routeDraftEditor: RouteDraftEditor,
) {
    internal fun registerMapTap(
        latitude: Double,
        longitude: Double,
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        state.update {
            it.copy(
                pendingAddLatitude = latitude,
                pendingAddLongitude = longitude,
                pendingAddFromMapTap = true,
                pendingAddAddress = null,
                favoriteNameInput = "",
                isFavoriteNameTouched = false,
            )
        }
        lookUpAddress(latitude, longitude, state, scope)
    }

    /** The address is looked up for a point with no name, and delivered into the state unless the add has ended or moved on. */
    private fun lookUpAddress(
        latitude: Double,
        longitude: Double,
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        addressLookup.start(scope, latitude, longitude) { address ->
            state.update { current ->
                current.copy(
                    pendingAddAddress = address,
                    favoriteNameInput = if (current.isFavoriteNameTouched) current.favoriteNameInput else address.short,
                )
            }
        }
    }

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
        recordAddedPlace(state.value, latitude, longitude)
        dismissAddWaypoint(state)
    }

    /** A waypoint tapped on the map is not a place the user chose by name, so it is remembered only if it was also saved as a favorite. */
    private fun recordAddedPlace(
        pending: RouteRequestState,
        latitude: Double,
        longitude: Double,
    ) {
        if (pending.pendingAddFromMapTap && !pending.isSaveAsFavoriteChecked) return
        val name = if (pending.isSaveAsFavoriteChecked) pending.favoriteNameInput else pending.pendingAddPlaceName
        recentPlacesRepository.record(NamedPlace(latitude, longitude, name))
    }

    internal fun dismissAddWaypoint(state: MutableStateFlow<RouteRequestState>) {
        addressLookup.cancel()
        state.update {
            it.copy(
                pendingAddLatitude = null,
                pendingAddLongitude = null,
                pendingAddPlaceName = null,
                pendingAddFromMapTap = false,
                pendingAddAddress = null,
                isSaveAsFavoriteChecked = false,
                favoriteNameInput = "",
                isFavoriteNameTouched = false,
            )
        }
    }

    internal fun startObservingPendingSearchWaypoint(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        pendingSearchWaypointReceiver.incoming
            .onEach { result ->
                val placeName = result.name.takeIf { it.isNotBlank() }
                state.update {
                    it.copy(
                        pendingAddLatitude = result.latitude,
                        pendingAddLongitude = result.longitude,
                        pendingAddPlaceName = placeName,
                        pendingAddFromMapTap = false,
                        pendingAddAddress = null,
                        favoriteNameInput = "",
                        isFavoriteNameTouched = false,
                    )
                }
                if (placeName == null) lookUpAddress(result.latitude, result.longitude, state, scope) else addressLookup.cancel()
            }.launchIn(scope)
    }
}
