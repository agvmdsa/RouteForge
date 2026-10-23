package com.routeforge.simulation.presentation

import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.holder.PendingTeleportTargetHolder
import com.routeforge.simulation.domain.usecase.IsNetworkConnectedUseCase
import com.routeforge.simulation.domain.usecase.TeleportUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class PendingTeleportConfirmer(
    private val teleportUseCase: TeleportUseCase,
    private val isNetworkConnectedUseCase: IsNetworkConnectedUseCase,
    private val pendingTeleportTargetHolder: PendingTeleportTargetHolder,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
    private val mockLocationAuthorizationGuard: MockLocationAuthorizationGuard,
) {
    internal fun registerMapTap(
        latitude: Double,
        longitude: Double,
        state: MutableStateFlow<SimulationState>,
    ) {
        state.update {
            it.copy(
                pendingTeleportLatitude = latitude,
                pendingTeleportLongitude = longitude,
                isPendingTeleportBlockedOffline = !isNetworkConnectedUseCase(),
            )
        }
    }

    internal fun cancelTeleport(state: MutableStateFlow<SimulationState>) {
        state.update {
            it.copy(
                pendingTeleportLatitude = null,
                pendingTeleportLongitude = null,
                isPendingTeleportBlockedOffline = false,
            )
        }
    }

    internal fun confirmTeleport(state: MutableStateFlow<SimulationState>) {
        val latitude = state.value.pendingTeleportLatitude
        val longitude = state.value.pendingTeleportLongitude
        val isBlockedOffline = state.value.isPendingTeleportBlockedOffline
        state.update {
            it.copy(
                pendingTeleportLatitude = null,
                pendingTeleportLongitude = null,
                isPendingTeleportBlockedOffline = false,
            )
        }
        if (latitude == null || longitude == null || isBlockedOffline) return
        teleportTo(latitude, longitude, state)
    }

    /** The favorite-teleport path defers to this confirmation only when a route is loaded or
     *  running — teleporting replaces the mocked session, so proceeding here cancels it. With no
     *  route loaded, [teleportTo] already runs directly from the holder observer in [init]. */
    internal fun confirmFavoriteTeleportCancelRoute(state: MutableStateFlow<SimulationState>) {
        val target = state.value.pendingFavoriteTeleportTarget ?: return
        state.update { it.copy(pendingFavoriteTeleportTarget = null) }
        lastComputedRouteHolder.clear()
        teleportTo(target.latitude, target.longitude, state)
    }

    /** Shared by [confirmTeleport] (map-tap path, already confirmed via TeleportConfirmationSheet),
     *  the [PendingTeleportTargetHolder] observer (favorite path with no route loaded — already
     *  confirmed on the Favorites screen, no second ask here), and
     *  [confirmFavoriteTeleportCancelRoute] (favorite path with a route loaded — that extra
     *  confirmation is specifically about cancelling the route, not a repeat of Favorites' own). */
    private fun teleportTo(
        latitude: Double,
        longitude: Double,
        state: MutableStateFlow<SimulationState>,
    ) {
        if (!mockLocationAuthorizationGuard.isAuthorized()) {
            mockLocationAuthorizationGuard.reportUnauthorized(state)
            return
        }
        state.update { it.copy(errorType = null, isBlockedByAuthorization = false) }
        teleportUseCase(latitude, longitude)
    }

    internal fun startObservingPendingTeleportTarget(
        state: MutableStateFlow<SimulationState>,
        scope: CoroutineScope,
    ) {
        pendingTeleportTargetHolder.target
            .onEach { point ->
                if (point != null) {
                    if (state.value.loadedRoute != null) {
                        state.update { it.copy(pendingFavoriteTeleportTarget = point) }
                    } else {
                        teleportTo(point.latitude, point.longitude, state)
                    }
                    pendingTeleportTargetHolder.clear()
                }
            }.launchIn(scope)
    }
}
