package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.PendingIncomingRouteHolder
import com.routeforge.coredomain.model.IncomingRoute
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.usecase.ImportRouteFileUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class IncomingRouteReceiver(
    private val importRouteFile: ImportRouteFileUseCase,
    private val pendingIncomingRouteHolder: PendingIncomingRouteHolder,
    private val routeDraftEditor: RouteDraftEditor,
) {
    /** A route another app handed over: it becomes the draft at once when nothing is placed yet, otherwise the user is asked
     *  first, so that opening a file never silently throws their waypoints away. A route that cannot be used shows the same
     *  error as the Import button and changes nothing. */
    private fun receiveIncomingRoute(
        route: IncomingRoute,
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        val points =
            when (route) {
                is IncomingRoute.Stops -> route.points
                is IncomingRoute.GpxFile ->
                    when (val result = importRouteFile(route.bytes, RouteFileFormat.GPX)) {
                        is Result.Success -> result.data.points
                        is Result.Error -> {
                            state.update { it.copy(errorType = result.error.toRouteRequestError()) }
                            return
                        }
                    }
            }
        if (points.size < MIN_WAYPOINTS_TO_PLAY) {
            state.update { it.copy(errorType = RouteRequestError.IMPORT_TOO_FEW_WAYPOINTS) }
            return
        }
        val arrived = RouteDraft(points = points)
        if (state.value.draft.points
                .isEmpty()
        ) {
            routeDraftEditor.mutateDraft(state, scope) { arrived }
        } else {
            state.update { it.copy(pendingReplaceDraft = arrived) }
        }
    }

    internal fun confirmReplaceDraft(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        val arrived = state.value.pendingReplaceDraft ?: return
        state.update { it.copy(pendingReplaceDraft = null) }
        routeDraftEditor.mutateDraft(state, scope) { arrived }
    }

    internal fun startObservingIncomingRoute(
        state: MutableStateFlow<RouteRequestState>,
        scope: CoroutineScope,
    ) {
        pendingIncomingRouteHolder.route
            .onEach { route ->
                if (route != null) {
                    receiveIncomingRoute(route, state, scope)
                    pendingIncomingRouteHolder.clear()
                }
            }.launchIn(scope)
    }
}
