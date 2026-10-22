package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.coredomain.usecase.PrepareRouteOptionsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class RouteModePreviewer(
    private val prepareRouteOptions: PrepareRouteOptionsUseCase,
    private val computeRequiredRegions: ComputeRequiredRegionsUseCase,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    internal fun refreshGuidedPreview(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        val points = state.value.draft.points
        if (points.size < 2) {
            state.update { it.copy(previewMode = null, previewRoute = null) }
            return
        }
        val requiredRegions = computeRequiredRegions(points)
        if (!requiredRegions.isFullyDownloaded) {
            state.update { it.copy(missingRegionsWarning = requiredRegions, pendingModePreviewTarget = RoutePlaybackMode.GUIDED) }
        } else {
            computeDraftPreview(points, state, scope)
        }
    }

    /** Lets the user preview/switch between Guided and Free-roam at any planning-time point —
     *  before an official route is computed (toggles [RouteRequestState.previewRoute], independent
     *  of [RouteRequestState.route], never opens the ready sheet), or after ([route] already
     *  computed). Neither case needs confirmation — switching modes while planning has no real
     *  consequence (nothing is being played). Only Simulate's execution-time switch asks to
     *  confirm, since that one affects a live session. */
    internal fun onSwitchRouteModeClick(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        val route = state.value.route
        if (route != null) {
            val targetMode = if (route.mode == RoutePlaybackMode.GUIDED) RoutePlaybackMode.FREE_ROAM else RoutePlaybackMode.GUIDED
            val alternateGeometry = route.alternateGeometry
            val alternateDistance = route.alternateDistanceMeters
            if (alternateGeometry != null && alternateDistance != null) {
                applyModeSwitch(route, targetMode, alternateGeometry, alternateDistance, state)
            } else {
                val requiredRegions = computeRequiredRegions(route.points)
                if (!requiredRegions.isFullyDownloaded) {
                    state.update { it.copy(missingRegionsWarning = requiredRegions, pendingModePreviewTarget = targetMode) }
                } else {
                    attemptModePreview(targetMode, state, scope)
                }
            }
            return
        }

        if (state.value.previewMode == RoutePlaybackMode.GUIDED) {
            state.update { it.copy(previewMode = null, previewRoute = null) }
            return
        }
        refreshGuidedPreview(state, scope)
    }

    /** Computes a live Guided preview for the current draft (no official route exists yet) — the
     *  missing-regions check, if needed, already ran in [onSwitchRouteModeClick] or was just
     *  dismissed via "continue anyway". On failure (genuinely no viable path), surfaces
     *  [RouteRequestError.MODE_PREVIEW_NO_PATH]. */
    internal fun computeDraftPreview(
        points: List<RoutePoint>,
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        state.update { it.copy(isComputingPreview = true) }
        scope.launch {
            val options = withContext(backgroundDispatcher) { prepareRouteOptions(points) }
            val guided = options.guided
            if (guided == null) {
                state.update { it.copy(isComputingPreview = false, errorType = RouteRequestError.MODE_PREVIEW_NO_PATH) }
                return@launch
            }
            state.update { it.copy(isComputingPreview = false, previewMode = RoutePlaybackMode.GUIDED, previewRoute = guided) }
        }
    }

    /** Computes [targetMode]'s geometry on demand (the missing-regions check, if needed, already ran
     *  in [onSwitchRouteModeClick] or was just dismissed via "continue anyway"). On success, applies
     *  it immediately via [applyModeSwitch] — no confirmation while planning, see
     *  [onSwitchRouteModeClick]. On failure (genuinely no viable path, not a download problem),
     *  surfaces [RouteRequestError.MODE_PREVIEW_NO_PATH] instead. */
    internal fun attemptModePreview(
        targetMode: RoutePlaybackMode,
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        val route = state.value.route ?: return
        state.update { it.copy(isComputingPreview = true) }
        scope.launch {
            val options = withContext(backgroundDispatcher) { prepareRouteOptions(route.points) }
            val targetRoute =
                when (targetMode) {
                    RoutePlaybackMode.GUIDED -> options.guided
                    RoutePlaybackMode.FREE_ROAM -> options.freeRoam
                }
            if (targetRoute == null) {
                state.update { it.copy(isComputingPreview = false, errorType = RouteRequestError.MODE_PREVIEW_NO_PATH) }
                return@launch
            }
            state.update { it.copy(isComputingPreview = false) }
            applyModeSwitch(route, targetMode, targetRoute.geometry, targetRoute.distanceMeters, state)
        }
    }

    private fun applyModeSwitch(
        route: Route,
        targetMode: RoutePlaybackMode,
        alternateGeometry: List<LatLng>,
        alternateDistanceMeters: Double,
        state: MutableStateFlow<RouteRequestState>,
    ) {
        val swapped = route.swapToMode(targetMode, alternateGeometry, alternateDistanceMeters)
        state.update { it.copy(route = swapped, chosenMode = targetMode) }
        lastComputedRouteHolder.set(swapped)
    }
}
