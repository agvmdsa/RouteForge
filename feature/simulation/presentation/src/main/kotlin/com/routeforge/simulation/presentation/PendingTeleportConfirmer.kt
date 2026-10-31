package com.routeforge.simulation.presentation

import com.routeforge.coredomain.AddressLookup
import com.routeforge.coredomain.RecentPlacesRepository
import com.routeforge.coredomain.handoff.LastComputedRouteHandoffClearer
import com.routeforge.coredomain.handoff.PendingTeleportTargetHandoffReceiver
import com.routeforge.coredomain.model.NamedPlace
import com.routeforge.simulation.domain.usecase.IsNetworkConnectedUseCase
import com.routeforge.simulation.domain.usecase.TeleportUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class PendingTeleportConfirmer(
    private val teleport: TeleportUseCase,
    private val isNetworkConnected: IsNetworkConnectedUseCase,
    private val addressLookup: AddressLookup,
    private val pendingTeleportTargetReceiver: PendingTeleportTargetHandoffReceiver,
    private val lastComputedRouteClearer: LastComputedRouteHandoffClearer,
    private val recentPlacesRepository: RecentPlacesRepository,
    private val mockLocationAuthorizationGuard: MockLocationAuthorizationGuard,
) {
    internal fun registerMapTap(
        latitude: Double,
        longitude: Double,
        state: MutableStateFlow<SimulationState>,
        scope: CoroutineScope,
    ) {
        state.update {
            it.copy(
                pendingTeleportLatitude = latitude,
                pendingTeleportLongitude = longitude,
                isPendingTeleportBlockedOffline = !isNetworkConnected(),
                pendingTeleportAddress = null,
            )
        }
        // A teleport blocked without a connection is not offered, so no address is asked for either.
        if (state.value.isPendingTeleportBlockedOffline) {
            addressLookup.cancel()
        } else {
            addressLookup.start(scope, latitude, longitude) { address -> state.update { it.copy(pendingTeleportAddress = address) } }
        }
    }

    internal fun cancelTeleport(state: MutableStateFlow<SimulationState>) {
        addressLookup.cancel()
        state.update {
            it.copy(
                pendingTeleportLatitude = null,
                pendingTeleportLongitude = null,
                isPendingTeleportBlockedOffline = false,
                pendingTeleportAddress = null,
            )
        }
    }

    internal fun confirmTeleport(state: MutableStateFlow<SimulationState>) {
        val latitude = state.value.pendingTeleportLatitude
        val longitude = state.value.pendingTeleportLongitude
        val isBlockedOffline = state.value.isPendingTeleportBlockedOffline
        addressLookup.cancel()
        state.update {
            it.copy(
                pendingTeleportLatitude = null,
                pendingTeleportLongitude = null,
                isPendingTeleportBlockedOffline = false,
                pendingTeleportAddress = null,
            )
        }
        if (latitude == null || longitude == null || isBlockedOffline) return
        teleportTo(latitude, longitude, name = null, state)
    }

    /** The favorite-teleport path defers to this confirmation only when a route is loaded or
     *  running — teleporting replaces the mocked session, so proceeding here cancels it. With no
     *  route loaded, [teleportTo] already runs directly from the observer of the pending teleport
     *  target ([startObservingPendingTeleportTarget], which the view model starts when it is
     *  created). */
    internal fun confirmFavoriteTeleportCancelRoute(state: MutableStateFlow<SimulationState>) {
        val target = state.value.pendingFavoriteTeleportTarget ?: return
        state.update { it.copy(pendingFavoriteTeleportTarget = null) }
        lastComputedRouteClearer.clear()
        teleportTo(target.latitude, target.longitude, target.name, state)
    }

    /** Shared by [confirmTeleport] (map-tap path, already confirmed via TeleportConfirmationSheet),
     *  the [PendingTeleportTargetHandoffReceiver] observer (favorite path with no route loaded — already
     *  confirmed on the Favorites screen, no second ask here), and
     *  [confirmFavoriteTeleportCancelRoute] (favorite path with a route loaded — that extra
     *  confirmation is specifically about cancelling the route, not a repeat of Favorites' own). */
    private fun teleportTo(
        latitude: Double,
        longitude: Double,
        name: String?,
        state: MutableStateFlow<SimulationState>,
    ) {
        if (!mockLocationAuthorizationGuard.isAuthorized()) {
            mockLocationAuthorizationGuard.reportUnauthorized(state)
            return
        }
        state.update { it.copy(errorType = null, isBlockedByAuthorization = false) }
        recentPlacesRepository.record(NamedPlace(latitude, longitude, name))
        teleport(latitude, longitude)
    }

    internal fun startObservingPendingTeleportTarget(
        state: MutableStateFlow<SimulationState>,
        scope: CoroutineScope,
    ) {
        pendingTeleportTargetReceiver.incoming
            .onEach { place ->
                if (state.value.loadedRoute != null) {
                    state.update { it.copy(pendingFavoriteTeleportTarget = place) }
                } else {
                    teleportTo(place.latitude, place.longitude, place.name, state)
                }
            }.launchIn(scope)
    }
}
