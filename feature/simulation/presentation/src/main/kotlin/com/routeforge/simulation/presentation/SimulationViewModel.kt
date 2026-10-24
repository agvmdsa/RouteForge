package com.routeforge.simulation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.usecase.ObserveMapStyleUseCase
import com.routeforge.simulation.domain.usecase.ObserveMockedSessionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SimulationViewModel(
    private val pendingTeleportConfirmer: PendingTeleportConfirmer,
    private val mockLocationAuthorizationGuard: MockLocationAuthorizationGuard,
    private val routeStarter: RouteStarter,
    private val joystickSessionController: JoystickSessionController,
    private val routeModeSwitcher: RouteModeSwitcher,
    private val playbackControls: PlaybackControls,
    private val realLocationTracker: RealLocationTracker,
    observeMockedSession: ObserveMockedSessionUseCase,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
    observeMapStyle: ObserveMapStyleUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SimulationState())
    val state = _state.asStateFlow()

    private val _events = Channel<SimulationEvent>()
    val events = _events.receiveAsFlow()

    init {
        observeMockedSession()
            .onEach { mockedSession ->
                _state.update { it.copy(mockedSession = mockedSession) }
                joystickSessionController.onMockedSessionChanged(mockedSession)
                if (mockedSession == null) {
                    realLocationTracker.startObservingRealLocation(_state, viewModelScope)
                } else {
                    realLocationTracker.stopObservingRealLocation()
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

        realLocationTracker.startObservingLastKnownLocation(_state, viewModelScope)

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
            SimulationAction.OnLocationPermissionGranted -> realLocationTracker.onLocationPermissionGranted(_state, viewModelScope)
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
}
