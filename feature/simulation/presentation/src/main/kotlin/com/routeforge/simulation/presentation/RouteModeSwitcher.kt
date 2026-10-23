package com.routeforge.simulation.presentation

import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.simulation.domain.usecase.UpdateActiveRouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class RouteModeSwitcher(
    private val updateActiveRouteUseCase: UpdateActiveRouteUseCase,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
) {
    /** Only proposes switching to whichever mode isn't currently active — there are just the two,
     *  so this is a toggle, not a picker. No-op if the loaded route never had both modes
     *  computed (nothing to switch to). */
    internal fun onSwitchRouteModeClick(state: MutableStateFlow<SimulationState>) {
        val route = state.value.loadedRoute ?: return
        if (route.alternateGeometry == null || route.alternateDistanceMeters == null) return
        val targetMode = if (route.mode == RoutePlaybackMode.GUIDED) RoutePlaybackMode.FREE_ROAM else RoutePlaybackMode.GUIDED
        state.update { it.copy(pendingRouteModeSwitch = targetMode) }
    }

    /** Swaps geometry/mode with their "alternate" counterparts (so switching back and forth keeps
     *  working) and pushes the result both to [lastComputedRouteHolder] (so the loaded-route UI
     *  updates whether or not playback has started yet) and, if a route is actively playing, to
     *  the controller's live session via [updateActiveRouteUseCase] — a no-op there otherwise.
     *  [Route.waypointCumulativeDistances] belongs to whichever geometry was active when it was
     *  computed, so it's cleared on swap (spec 008) — the editable-range check safely treats an
     *  empty list as "unknown, treat everything as editable" until the next edit recomputes it for
     *  the newly active mode. */
    internal fun confirmSwitchRouteMode(state: MutableStateFlow<SimulationState>) {
        val targetMode = state.value.pendingRouteModeSwitch ?: return
        state.update { it.copy(pendingRouteModeSwitch = null) }
        val route = state.value.loadedRoute ?: return
        val alternateGeometry = route.alternateGeometry ?: return
        val alternateDistance = route.alternateDistanceMeters ?: return
        val swapped = route.swapToMode(targetMode, alternateGeometry, alternateDistance)
        lastComputedRouteHolder.set(swapped)
        updateActiveRouteUseCase(swapped)
    }

    /** Applies a committed waypoint edit (spec 008) exactly like [confirmSwitchRouteMode] applies
     *  a mode swap: pushes the recomputed [route] to [lastComputedRouteHolder] (so the loaded-route
     *  UI updates whether or not playback has started) and, if a session is actively playing, to
     *  the controller's live session via [updateActiveRouteUseCase] — a no-op there otherwise. */
    internal fun applyEditedRoute(route: Route) {
        lastComputedRouteHolder.set(route)
        updateActiveRouteUseCase(route)
    }
}
