package com.routeforge.simulation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.holder.LastKnownRealLocationHolder
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.usecase.ObserveMapStyleUseCase
import com.routeforge.simulation.domain.RealLocationFailure
import com.routeforge.simulation.domain.RealLocationUpdate
import com.routeforge.simulation.domain.usecase.ObserveMockedSessionUseCase
import com.routeforge.simulation.domain.usecase.ObserveRealLocationUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val REAL_LOCATION_TIMEOUT_MILLIS = 30_000L

internal class SimulationViewModel(
    private val pendingTeleportConfirmer: PendingTeleportConfirmer,
    private val mockLocationAuthorizationGuard: MockLocationAuthorizationGuard,
    private val routeStarter: RouteStarter,
    private val joystickSessionController: JoystickSessionController,
    private val routeModeSwitcher: RouteModeSwitcher,
    private val playbackControls: PlaybackControls,
    observeMockedSessionUseCase: ObserveMockedSessionUseCase,
    private val observeRealLocationUseCase: ObserveRealLocationUseCase,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
    private val lastKnownRealLocationHolder: LastKnownRealLocationHolder,
    observeMapStyle: ObserveMapStyleUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SimulationState())
    val state = _state.asStateFlow()

    private val _events = Channel<SimulationEvent>()
    val events = _events.receiveAsFlow()

    private var realLocationObservationJob: Job? = null
    private var realLocationTimeoutJob: Job? = null

    init {
        observeMockedSessionUseCase()
            .onEach { mockedSession ->
                _state.update { it.copy(mockedSession = mockedSession) }
                joystickSessionController.onMockedSessionChanged(mockedSession)
                if (mockedSession == null) {
                    startObservingRealLocation()
                } else {
                    stopObservingRealLocation()
                }
            }.launchIn(viewModelScope)

        mockLocationAuthorizationGuard
            .authorizationLost()
            .onEach { _events.send(SimulationEvent.NavigateToSetup) }
            .launchIn(viewModelScope)

        observeMapStyle()
            .onEach { mapStyle -> _state.update { it.copy(mapStyle = mapStyle) } }
            .launchIn(viewModelScope)

        lastComputedRouteHolder.route
            .onEach { route -> _state.update { it.copy(loadedRoute = route) } }
            .launchIn(viewModelScope)

        lastKnownRealLocationHolder.location
            .onEach { location -> _state.update { it.copy(realLocation = location) } }
            .launchIn(viewModelScope)

        pendingTeleportConfirmer.startObservingPendingTeleportTarget(_state, viewModelScope)
    }

    fun onAction(action: SimulationAction) {
        when (action) {
            is SimulationAction.OnMapTap -> pendingTeleportConfirmer.registerMapTap(action.latitude, action.longitude, _state)
            SimulationAction.OnConfirmTeleport -> pendingTeleportConfirmer.confirmTeleport(_state)
            SimulationAction.OnCancelTeleport -> pendingTeleportConfirmer.cancelTeleport(_state)
            is SimulationAction.OnPlaybackSpeedChange -> playbackControls.onPlaybackSpeedChange(action.kmh, _state)
            is SimulationAction.OnExecutionModeSelected -> _state.update { it.copy(executionModeSelection = action.selection) }
            is SimulationAction.OnExecutionTimesInputChange -> _state.update { it.copy(executionTimesInput = action.value) }
            SimulationAction.OnStartRouteSimulation -> _state.update { it.copy(isStartRouteDialogOpen = true) }
            SimulationAction.OnConfirmStartRoute -> routeStarter.confirmStartRoute(_state)
            SimulationAction.OnDismissStartRouteDialog ->
                _state.update { it.copy(isStartRouteDialogOpen = false, errorType = null) }
            SimulationAction.OnPauseSimulation -> playbackControls.pause()
            SimulationAction.OnResumeSimulation -> playbackControls.resume()
            SimulationAction.OnStopSimulation -> playbackControls.stop(_state)
            SimulationAction.OnCancelRouteClick -> _state.update { it.copy(isPendingCancelRoute = true) }
            SimulationAction.OnDismissCancelRoute -> _state.update { it.copy(isPendingCancelRoute = false) }
            is SimulationAction.OnJoystickDrag -> joystickSessionController.onJoystickDrag(action.bearingDegrees, _state)
            SimulationAction.OnJoystickReleased -> joystickSessionController.onJoystickReleased()
            SimulationAction.OnToggleJoystick -> joystickSessionController.onToggleJoystick(_state)
            is SimulationAction.OnJoystickSpeedChange -> joystickSessionController.onJoystickSpeedChange(action.kmh, _state)
            SimulationAction.OnConfirmJoystickInterrupt -> joystickSessionController.confirmJoystickInterrupt(_state)
            SimulationAction.OnDismissJoystickInterrupt -> _state.update { it.copy(isJoystickInterruptPending = false) }
            SimulationAction.OnCancelMockClick -> _state.update { it.copy(isPendingCancelMock = true) }
            SimulationAction.OnConfirmCancelMock -> playbackControls.confirmCancelMock(_state)
            SimulationAction.OnDismissCancelMock -> _state.update { it.copy(isPendingCancelMock = false) }
            SimulationAction.OnOpenSetupClick ->
                viewModelScope.launch { _events.send(SimulationEvent.NavigateToSetup) }
            SimulationAction.OnLocationPermissionGranted ->
                if (_state.value.mockedSession == null) startObservingRealLocation()
            SimulationAction.OnScreenResumed -> checkAuthorizationStillGranted()
            SimulationAction.OnConfirmFavoriteTeleportCancelRoute -> pendingTeleportConfirmer.confirmFavoriteTeleportCancelRoute(_state)
            SimulationAction.OnDismissFavoriteTeleportCancelRoute ->
                _state.update { it.copy(pendingFavoriteTeleportTarget = null) }
            SimulationAction.OnSwitchRouteModeClick -> routeModeSwitcher.onSwitchRouteModeClick(_state)
            SimulationAction.OnConfirmSwitchRouteMode -> routeModeSwitcher.confirmSwitchRouteMode(_state)
            SimulationAction.OnDismissSwitchRouteMode -> _state.update { it.copy(pendingRouteModeSwitch = null) }
        }
    }

    fun applyEditedRoute(route: Route) = routeModeSwitcher.applyEditedRoute(route)

    /** Mock-location authorization can be revoked at any time from outside the app (Developer
     *  Options, an MDM policy, the user picking a different mock-location app) — catch that here
     *  instead of only at the next explicit action, and send the user back to setup to fix it. */
    private fun checkAuthorizationStillGranted() {
        if (!mockLocationAuthorizationGuard.isAuthorized()) {
            viewModelScope.launch { _events.send(SimulationEvent.NavigateToSetup) }
        }
    }

    private fun startObservingRealLocation() {
        if (realLocationObservationJob?.isActive == true) return
        _state.update { it.copy(isSearchingRealLocation = true, errorType = it.errorType.clearIfRealLocationRelated()) }
        realLocationTimeoutJob =
            viewModelScope.launch {
                delay(REAL_LOCATION_TIMEOUT_MILLIS)
                if (_state.value.isSearchingRealLocation) {
                    _state.update {
                        it.copy(isSearchingRealLocation = false, errorType = SimulationErrorType.REAL_LOCATION_TIMED_OUT)
                    }
                }
            }
        realLocationObservationJob =
            observeRealLocationUseCase()
                .onEach { update ->
                    realLocationTimeoutJob?.cancel()
                    when (update) {
                        is RealLocationUpdate.Fix -> {
                            lastKnownRealLocationHolder.set(update.location)
                            _state.update {
                                it.copy(isSearchingRealLocation = false, errorType = it.errorType.clearIfRealLocationRelated())
                            }
                        }
                        is RealLocationUpdate.Unavailable -> {
                            _state.update {
                                it.copy(isSearchingRealLocation = false, errorType = update.reason.toSimulationErrorType())
                            }
                        }
                    }
                }.launchIn(viewModelScope)
    }

    private fun stopObservingRealLocation() {
        realLocationObservationJob?.cancel()
        realLocationObservationJob = null
        realLocationTimeoutJob?.cancel()
        realLocationTimeoutJob = null
    }

    private fun RealLocationFailure.toSimulationErrorType(): SimulationErrorType =
        when (this) {
            RealLocationFailure.ProviderDisabled -> SimulationErrorType.REAL_LOCATION_PROVIDER_DISABLED
            RealLocationFailure.PermissionDenied -> SimulationErrorType.REAL_LOCATION_PERMISSION_DENIED
        }

    /** Clears a real-location-related error once a search (re)starts or resolves — leaves any
     *  unrelated error (e.g. NOT_AUTHORIZED) untouched. */
    private fun SimulationErrorType?.clearIfRealLocationRelated(): SimulationErrorType? =
        when (this) {
            SimulationErrorType.REAL_LOCATION_PROVIDER_DISABLED,
            SimulationErrorType.REAL_LOCATION_PERMISSION_DENIED,
            SimulationErrorType.REAL_LOCATION_TIMED_OUT,
            -> null
            else -> this
        }
}
