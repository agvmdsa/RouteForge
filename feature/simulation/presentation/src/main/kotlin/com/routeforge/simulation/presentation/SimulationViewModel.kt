package com.routeforge.simulation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.coredomain.MockLocationAuthorizationChecker
import com.routeforge.simulation.domain.LastKnownRealLocationHolder
import com.routeforge.simulation.domain.usecase.IsNetworkConnectedUseCase
import com.routeforge.simulation.domain.usecase.ObserveMockedSessionUseCase
import com.routeforge.simulation.domain.usecase.ObserveRealLocationUseCase
import com.routeforge.simulation.domain.usecase.PauseSimulationUseCase
import com.routeforge.simulation.domain.usecase.ResumeSimulationUseCase
import com.routeforge.simulation.domain.usecase.StartRouteSimulationUseCase
import com.routeforge.simulation.domain.usecase.StopSimulationUseCase
import com.routeforge.simulation.domain.usecase.TeleportUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SimulationViewModel(
    private val teleportUseCase: TeleportUseCase,
    private val startRouteSimulationUseCase: StartRouteSimulationUseCase,
    private val pauseSimulationUseCase: PauseSimulationUseCase,
    private val resumeSimulationUseCase: ResumeSimulationUseCase,
    private val stopSimulationUseCase: StopSimulationUseCase,
    observeMockedSessionUseCase: ObserveMockedSessionUseCase,
    private val observeRealLocationUseCase: ObserveRealLocationUseCase,
    private val isNetworkConnectedUseCase: IsNetworkConnectedUseCase,
    private val mockLocationAuthorizationChecker: MockLocationAuthorizationChecker,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
    private val lastKnownRealLocationHolder: LastKnownRealLocationHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(SimulationState())
    val state = _state.asStateFlow()

    private val _events = Channel<SimulationEvent>()
    val events = _events.receiveAsFlow()

    private var realLocationObservationJob: Job? = null

    init {
        observeMockedSessionUseCase()
            .onEach { mockedSession ->
                _state.update { it.copy(mockedSession = mockedSession) }
                if (mockedSession == null) {
                    startObservingRealLocation()
                } else {
                    stopObservingRealLocation()
                }
            }.launchIn(viewModelScope)

        lastComputedRouteHolder.route
            .onEach { route -> _state.update { it.copy(loadedRoute = route) } }
            .launchIn(viewModelScope)

        lastKnownRealLocationHolder.location
            .onEach { location -> _state.update { it.copy(realLocation = location) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: SimulationAction) {
        when (action) {
            is SimulationAction.OnMapTap ->
                _state.update {
                    it.copy(
                        pendingTeleportLatitude = action.latitude,
                        pendingTeleportLongitude = action.longitude,
                        isPendingTeleportBlockedOffline = !isNetworkConnectedUseCase(),
                    )
                }
            SimulationAction.OnConfirmTeleport -> confirmTeleport()
            SimulationAction.OnCancelTeleport ->
                _state.update {
                    it.copy(
                        pendingTeleportLatitude = null,
                        pendingTeleportLongitude = null,
                        isPendingTeleportBlockedOffline = false,
                    )
                }
            is SimulationAction.OnSpeedInputChange -> _state.update { it.copy(speedInput = action.value) }
            SimulationAction.OnStartRouteSimulation -> startRouteSimulation()
            SimulationAction.OnPauseSimulation -> pauseSimulationUseCase()
            SimulationAction.OnResumeSimulation -> resumeSimulationUseCase()
            SimulationAction.OnStopSimulation -> stopSimulationUseCase()
            SimulationAction.OnCancelMockClick -> _state.update { it.copy(isPendingCancelMock = true) }
            SimulationAction.OnConfirmCancelMock -> {
                _state.update { it.copy(isPendingCancelMock = false) }
                stopSimulationUseCase()
            }
            SimulationAction.OnDismissCancelMock -> _state.update { it.copy(isPendingCancelMock = false) }
            SimulationAction.OnPlanRouteClick ->
                viewModelScope.launch { _events.send(SimulationEvent.NavigateToPlanRoute) }
            SimulationAction.OnOpenSetupClick ->
                viewModelScope.launch { _events.send(SimulationEvent.NavigateToSetup) }
            SimulationAction.OnLocationPermissionGranted ->
                if (_state.value.mockedSession == null) startObservingRealLocation()
        }
    }

    private fun confirmTeleport() {
        val latitude = _state.value.pendingTeleportLatitude
        val longitude = _state.value.pendingTeleportLongitude
        val isBlockedOffline = _state.value.isPendingTeleportBlockedOffline
        _state.update {
            it.copy(
                pendingTeleportLatitude = null,
                pendingTeleportLongitude = null,
                isPendingTeleportBlockedOffline = false,
            )
        }
        if (latitude == null || longitude == null || isBlockedOffline) return

        if (!mockLocationAuthorizationChecker.isAuthorized()) {
            reportUnauthorized()
            return
        }
        _state.update { it.copy(errorType = null, isBlockedByAuthorization = false) }
        teleportUseCase(latitude, longitude)
    }

    private fun startRouteSimulation() {
        val route = _state.value.loadedRoute ?: return
        val speed = _state.value.speedInput.toFloatOrNull()
        if (speed == null || speed <= 0f) {
            _state.update { it.copy(errorType = SimulationErrorType.INVALID_SPEED) }
            return
        }

        if (!mockLocationAuthorizationChecker.isAuthorized()) {
            reportUnauthorized()
            return
        }
        _state.update { it.copy(errorType = null, isBlockedByAuthorization = false) }
        startRouteSimulationUseCase(route, speed)
    }

    private fun reportUnauthorized() {
        _state.update {
            it.copy(
                errorType = SimulationErrorType.NOT_AUTHORIZED,
                isBlockedByAuthorization = true,
            )
        }
    }

    private fun startObservingRealLocation() {
        if (realLocationObservationJob?.isActive == true) return
        _state.update { it.copy(isSearchingRealLocation = true) }
        realLocationObservationJob =
            observeRealLocationUseCase()
                .onEach { location ->
                    lastKnownRealLocationHolder.set(location)
                    _state.update { it.copy(isSearchingRealLocation = false) }
                }.launchIn(viewModelScope)
    }

    private fun stopObservingRealLocation() {
        realLocationObservationJob?.cancel()
        realLocationObservationJob = null
    }
}
