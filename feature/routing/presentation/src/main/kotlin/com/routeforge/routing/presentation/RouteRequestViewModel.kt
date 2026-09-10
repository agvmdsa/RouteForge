package com.routeforge.routing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.RoutingFailure
import com.routeforge.routing.domain.usecase.ComputeRouteUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RouteRequestViewModel(
    private val computeRoute: ComputeRouteUseCase,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _state = MutableStateFlow(RouteRequestState())
    val state = _state.asStateFlow()

    private val _events = Channel<RouteRequestEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: RouteRequestAction) {
        when (action) {
            is RouteRequestAction.OnStartLatitudeChange -> _state.update { it.copy(startLatitudeInput = action.value) }
            is RouteRequestAction.OnStartLongitudeChange -> _state.update { it.copy(startLongitudeInput = action.value) }
            is RouteRequestAction.OnEndLatitudeChange -> _state.update { it.copy(endLatitudeInput = action.value) }
            is RouteRequestAction.OnEndLongitudeChange -> _state.update { it.copy(endLongitudeInput = action.value) }
            RouteRequestAction.OnAddWaypoint -> _state.update { it.copy(waypoints = it.waypoints + WaypointInput()) }
            is RouteRequestAction.OnRemoveWaypoint ->
                _state.update { current ->
                    current.copy(waypoints = current.waypoints.filterIndexed { index, _ -> index != action.index })
                }
            is RouteRequestAction.OnWaypointLatitudeChange ->
                updateWaypoint(action.index) { it.copy(latitudeInput = action.value) }
            is RouteRequestAction.OnWaypointLongitudeChange ->
                updateWaypoint(action.index) { it.copy(longitudeInput = action.value) }
            RouteRequestAction.OnRequestRoute -> requestRoute()
            RouteRequestAction.OnOpenRegionCatalog ->
                viewModelScope.launch { _events.send(RouteRequestEvent.NavigateToRegionCatalog) }
        }
    }

    private fun updateWaypoint(
        index: Int,
        transform: (WaypointInput) -> WaypointInput,
    ) {
        _state.update { current ->
            current.copy(
                waypoints = current.waypoints.mapIndexed { i, waypoint -> if (i == index) transform(waypoint) else waypoint },
            )
        }
    }

    private fun requestRoute() {
        val points = _state.value.toRoutePointsOrNull()
        if (points == null) {
            _state.update { it.copy(errorMessage = "Enter valid coordinates for every point.") }
            return
        }

        _state.update { it.copy(isComputing = true, errorMessage = null) }
        viewModelScope.launch {
            val result = withContext(backgroundDispatcher) { computeRoute(points) }
            when (result) {
                is Result.Success ->
                    _state.update { it.copy(isComputing = false, route = result.data, errorMessage = null) }
                is Result.Error ->
                    _state.update { it.copy(isComputing = false, route = null, errorMessage = result.error.toUserMessage()) }
            }
            if (result is Result.Success) {
                lastComputedRouteHolder.set(result.data)
                _events.send(RouteRequestEvent.RouteComputed(result.data))
            }
        }
    }
}

private fun RouteRequestState.toRoutePointsOrNull(): List<RoutePoint>? {
    val start = parsePoint(startLatitudeInput, startLongitudeInput) ?: return null
    val end = parsePoint(endLatitudeInput, endLongitudeInput) ?: return null
    val middle = waypoints.map { parsePoint(it.latitudeInput, it.longitudeInput) ?: return null }
    return listOf(start) + middle + end
}

private fun parsePoint(
    latitudeInput: String,
    longitudeInput: String,
): RoutePoint? {
    val latitude = latitudeInput.toDoubleOrNull() ?: return null
    val longitude = longitudeInput.toDoubleOrNull() ?: return null
    return RoutePoint(latitude = latitude, longitude = longitude)
}

private fun RoutingFailure.toUserMessage(): String =
    when (this) {
        is RoutingFailure.PointNotRoutable -> "That point is too far from any known road to route from it."
        is RoutingFailure.PointOutsideAvailableCoverage ->
            "That point is outside any area with offline map data. Download its region first."
        RoutingFailure.NoPathBetweenPoints -> "No route could be found between those points."
    }
