package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RouteOptions
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.model.autoResolved
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.coredomain.usecase.PrepareRouteOptionsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class RouteComputer(
    private val prepareRouteOptions: PrepareRouteOptionsUseCase,
    private val computeRequiredRegions: ComputeRequiredRegionsUseCase,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    /** FR + region-awareness: before computing, checks whether the map data Guided mode needs is
     *  actually downloaded, so the user learns about missing regions up front instead of Guided
     *  silently failing to compute later. */
    internal fun requestRoute(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        val points = state.value.draft.points
        if (points.size < MIN_WAYPOINTS_TO_PLAY) {
            state.update { it.copy(errorType = RouteRequestError.TOO_FEW_WAYPOINTS_TO_PLAY) }
            return
        }

        val requiredRegions = computeRequiredRegions(points)
        if (!requiredRegions.isFullyDownloaded) {
            state.update { it.copy(missingRegionsWarning = requiredRegions) }
            return
        }
        proceedWithRouteComputation(points, state, scope)
    }

    internal fun proceedWithRouteComputation(
        points: List<RoutePoint>,
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        // FR-004: a fresh evaluation always clears any previously locked mode/choice. The mode the
        // user already picked via the preview switch (or Free-roam, its implicit default) carries
        // straight through instead of asking again with a mode-choice sheet — they already made
        // this call once.
        val preferredMode = state.value.previewMode ?: RoutePlaybackMode.FREE_ROAM
        state.update {
            it.copy(
                isComputing = true,
                errorType = null,
                chosenMode = null,
                route = null,
                previewMode = null,
                previewRoute = null,
            )
        }
        scope.launch {
            val options = withContext(backgroundDispatcher) { prepareRouteOptions(points) }
            val autoResolved = options.autoResolved
            if (autoResolved != null) {
                // FR-003: Guided isn't computable — skip straight to Free-roam, no choice shown.
                finalizeRoute(autoResolved.first, autoResolved.second, options, state)
            } else if (preferredMode == RoutePlaybackMode.GUIDED && options.guided != null) {
                finalizeRoute(options.guided!!, RoutePlaybackMode.GUIDED, options, state)
            } else {
                finalizeRoute(options.freeRoam, RoutePlaybackMode.FREE_ROAM, options, state)
            }
        }
    }

    private fun finalizeRoute(
        route: Route,
        mode: RoutePlaybackMode,
        options: RouteOptions,
        state: MutableStateFlow<RouteRequestState>,
    ) {
        val withAlternate =
            when (mode) {
                RoutePlaybackMode.GUIDED ->
                    route.copy(alternateGeometry = options.freeRoam.geometry, alternateDistanceMeters = options.freeRoam.distanceMeters)
                RoutePlaybackMode.FREE_ROAM ->
                    route.copy(alternateGeometry = options.guided?.geometry, alternateDistanceMeters = options.guided?.distanceMeters)
            }
        state.update { it.copy(isComputing = false, chosenMode = mode, route = withAlternate) }
        lastComputedRouteHolder.set(withAlternate)
    }
}
