package com.routeforge.simulation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.coredomain.LastKnownRealLocationHolder
import com.routeforge.coredomain.MockLocationAuthorizationChecker
import com.routeforge.coredomain.Result
import com.routeforge.simulation.domain.model.ExecutionMode
import com.routeforge.simulation.domain.model.JoystickInterruptDecision
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationStatus
import com.routeforge.simulation.domain.model.SpeedSetting
import com.routeforge.simulation.domain.usecase.ConfirmJoystickInterruptUseCase
import com.routeforge.simulation.domain.usecase.IsNetworkConnectedUseCase
import com.routeforge.simulation.domain.usecase.ObserveMockedSessionUseCase
import com.routeforge.simulation.domain.usecase.ObserveRealLocationUseCase
import com.routeforge.simulation.domain.usecase.PauseSimulationUseCase
import com.routeforge.simulation.domain.usecase.RequestJoystickInterruptUseCase
import com.routeforge.simulation.domain.usecase.ResumeSimulationUseCase
import com.routeforge.simulation.domain.usecase.SetSpeedUseCase
import com.routeforge.simulation.domain.usecase.StartRouteSimulationUseCase
import com.routeforge.simulation.domain.usecase.StopSimulationUseCase
import com.routeforge.simulation.domain.usecase.TeleportUseCase
import com.routeforge.simulation.domain.usecase.UpdateJoystickDirectionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val KMH_TO_MPS_DIVISOR = 3.6f

class SimulationViewModel(
    private val teleportUseCase: TeleportUseCase,
    private val startRouteSimulationUseCase: StartRouteSimulationUseCase,
    private val pauseSimulationUseCase: PauseSimulationUseCase,
    private val resumeSimulationUseCase: ResumeSimulationUseCase,
    private val stopSimulationUseCase: StopSimulationUseCase,
    observeMockedSessionUseCase: ObserveMockedSessionUseCase,
    private val observeRealLocationUseCase: ObserveRealLocationUseCase,
    private val isNetworkConnectedUseCase: IsNetworkConnectedUseCase,
    private val setSpeedUseCase: SetSpeedUseCase,
    private val requestJoystickInterruptUseCase: RequestJoystickInterruptUseCase,
    private val confirmJoystickInterruptUseCase: ConfirmJoystickInterruptUseCase,
    private val updateJoystickDirectionUseCase: UpdateJoystickDirectionUseCase,
    private val mockLocationAuthorizationChecker: MockLocationAuthorizationChecker,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
    private val lastKnownRealLocationHolder: LastKnownRealLocationHolder,
) : ViewModel() {
    private val _state = MutableStateFlow(SimulationState())
    val state = _state.asStateFlow()

    private val _events = Channel<SimulationEvent>()
    val events = _events.receiveAsFlow()

    private var realLocationObservationJob: Job? = null

    /**
     * Tracks locally (synchronously) whether the current joystick drag has already started a
     * session, instead of relying on [SimulationState.mockedSession] — that mirror updates
     * asynchronously via [observeMockedSessionUseCase], and drag events fire many times per
     * second. Checking the mirrored state let every frame of a drag think no session existed
     * yet, so `startJoystick()` (and its `SimulationControllerImpl.startSession()`, which cancels
     * and relaunches the tick loop) fired on *every* frame — the tick loop's 1-second delay never
     * got a chance to complete, so the mocked position never actually moved.
     */
    private var joystickSessionStarted = false

    init {
        observeMockedSessionUseCase()
            .onEach { mockedSession ->
                _state.update { it.copy(mockedSession = mockedSession) }
                if (mockedSession?.mode != SimulationMode.JOYSTICK) {
                    joystickSessionStarted = false
                }
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
            is SimulationAction.OnPlaybackSpeedChange -> onPlaybackSpeedChange(action.kmh)
            is SimulationAction.OnExecutionModeSelected -> _state.update { it.copy(executionModeSelection = action.selection) }
            is SimulationAction.OnExecutionTimesInputChange -> _state.update { it.copy(executionTimesInput = action.value) }
            SimulationAction.OnStartRouteSimulation -> _state.update { it.copy(isStartRouteDialogOpen = true) }
            SimulationAction.OnConfirmStartRoute -> confirmStartRoute()
            SimulationAction.OnDismissStartRouteDialog ->
                _state.update { it.copy(isStartRouteDialogOpen = false, errorType = null) }
            SimulationAction.OnPauseSimulation -> pauseSimulationUseCase()
            SimulationAction.OnResumeSimulation -> resumeSimulationUseCase()
            SimulationAction.OnStopSimulation -> {
                _state.update { it.copy(isPendingCancelRoute = false) }
                stopSimulationUseCase()
                lastComputedRouteHolder.clear()
            }
            SimulationAction.OnCancelRouteClick -> _state.update { it.copy(isPendingCancelRoute = true) }
            SimulationAction.OnDismissCancelRoute -> _state.update { it.copy(isPendingCancelRoute = false) }
            is SimulationAction.OnJoystickDrag -> onJoystickDrag(action.bearingDegrees)
            SimulationAction.OnJoystickReleased -> pauseSimulationUseCase()
            SimulationAction.OnToggleJoystick -> onToggleJoystick()
            is SimulationAction.OnJoystickSpeedChange -> onJoystickSpeedChange(action.kmh)
            SimulationAction.OnConfirmJoystickInterrupt -> confirmJoystickInterrupt()
            SimulationAction.OnDismissJoystickInterrupt -> _state.update { it.copy(isJoystickInterruptPending = false) }
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
            SimulationAction.OnScreenResumed -> checkAuthorizationStillGranted()
        }
    }

    /** Mock-location authorization can be revoked at any time from outside the app (Developer
     *  Options, an MDM policy, the user picking a different mock-location app) — catch that here
     *  instead of only at the next explicit action, and send the user back to setup to fix it. */
    private fun checkAuthorizationStillGranted() {
        if (!mockLocationAuthorizationChecker.isAuthorized()) {
            viewModelScope.launch { _events.send(SimulationEvent.NavigateToSetup) }
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

    private fun onPlaybackSpeedChange(kmh: Float) {
        _state.update { it.copy(playbackSpeedKmh = kmh) }
        applyLiveSpeed(SpeedSetting.Manual(kmh / KMH_TO_MPS_DIVISOR))
    }

    /** FR-016: only takes effect immediately if a session is already active; otherwise it's just staged for the next start. */
    private fun applyLiveSpeed(speed: SpeedSetting) {
        if (_state.value.mockedSession != null) setSpeedUseCase(speed)
    }

    /** Called from the "how many times" dialog's confirm button — validates and, if everything
     *  checks out, actually starts playback and closes the dialog. An invalid times count keeps
     *  the dialog open (the fix belongs right there); an authorization failure closes it, since
     *  that's resolved elsewhere (the setup screen). */
    private fun confirmStartRoute() {
        val route = _state.value.loadedRoute ?: return
        val speedKmh = _state.value.playbackSpeedKmh
        if (speedKmh <= 0f) {
            _state.update { it.copy(isStartRouteDialogOpen = false, errorType = SimulationErrorType.INVALID_SPEED) }
            return
        }
        val executionMode = resolveExecutionMode()
        if (executionMode == null) {
            _state.update { it.copy(errorType = SimulationErrorType.INVALID_EXECUTION_TIMES) }
            return
        }

        if (!mockLocationAuthorizationChecker.isAuthorized()) {
            _state.update { it.copy(isStartRouteDialogOpen = false) }
            reportUnauthorized()
            return
        }
        _state.update { it.copy(isStartRouteDialogOpen = false, errorType = null, isBlockedByAuthorization = false) }
        startRouteSimulationUseCase(route, SpeedSetting.Manual(speedKmh / KMH_TO_MPS_DIVISOR), executionMode)
    }

    private fun resolveExecutionMode(): ExecutionMode? =
        when (_state.value.executionModeSelection) {
            ExecutionModeSelection.ONCE -> ExecutionMode.Once
            ExecutionModeSelection.LOOP -> ExecutionMode.Loop
            ExecutionModeSelection.TIMES -> {
                val count = _state.value.executionTimesInput.toIntOrNull()
                if (count == null || count <= 0) null else ExecutionMode.Times(count)
            }
        }

    /**
     * FR-022/FR-024–FR-026 (revised): the joystick's visibility is an explicit toggle, separate
     * from whether it is actually mocking a position yet (that only starts once the user drags —
     * see [onJoystickDrag]). Turning it on while a route is loaded or actively playing requires
     * confirmation, because confirming clears the loaded route entirely. Turning it off while it
     * was actively mocking fully stops the session, returning to the real location.
     */
    private fun onJoystickDrag(bearingDegrees: Float) {
        if (!joystickSessionStarted) {
            when (confirmJoystickInterruptUseCase()) {
                is Result.Success -> {
                    joystickSessionStarted = true
                    _state.update { it.copy(errorType = null) }
                    setSpeedUseCase(SpeedSetting.Manual(_state.value.joystickSpeedKmh / KMH_TO_MPS_DIVISOR))
                }
                is Result.Error -> {
                    _state.update { it.copy(errorType = SimulationErrorType.JOYSTICK_NO_REAL_FIX) }
                    return
                }
            }
        } else if (_state.value.mockedSession?.status == SimulationStatus.PAUSED) {
            // A prior drag gesture ended (which pauses in place, per FR "releasing stops in
            // place"); a *new* drag gesture on an already-started session must resume it, or
            // every drag after the first release would silently update direction with the
            // session still paused and never actually move.
            resumeSimulationUseCase()
        }
        updateJoystickDirectionUseCase(bearingDegrees)
    }

    private fun onToggleJoystick() {
        if (_state.value.isJoystickVisible) {
            if (_state.value.mockedSession?.mode == SimulationMode.JOYSTICK) {
                stopSimulationUseCase()
            }
            joystickSessionStarted = false
            _state.update { it.copy(isJoystickVisible = false) }
            return
        }

        val routeIsLoadedOrRunning =
            _state.value.loadedRoute != null ||
                requestJoystickInterruptUseCase() == JoystickInterruptDecision.ConfirmationRequired
        if (routeIsLoadedOrRunning) {
            _state.update { it.copy(isJoystickInterruptPending = true) }
        } else {
            _state.update { it.copy(isJoystickVisible = true) }
        }
    }

    /** The joystick's own speed control, always available while it's visible — no need to load a route. */
    private fun onJoystickSpeedChange(kmh: Float) {
        _state.update { it.copy(joystickSpeedKmh = kmh) }
        applyLiveSpeed(SpeedSetting.Manual(kmh / KMH_TO_MPS_DIVISOR))
    }

    private fun confirmJoystickInterrupt() {
        lastComputedRouteHolder.clear()
        stopSimulationUseCase()
        joystickSessionStarted = false
        _state.update { it.copy(isJoystickInterruptPending = false, isJoystickVisible = true) }
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
