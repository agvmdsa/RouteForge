package com.routeforge.simulation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.holder.LastKnownRealLocationHolder
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.usecase.ObserveMapStyleUseCase
import com.routeforge.simulation.domain.RealLocationFailure
import com.routeforge.simulation.domain.RealLocationUpdate
import com.routeforge.simulation.domain.model.JoystickInterruptDecision
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationStatus
import com.routeforge.simulation.domain.model.SpeedSetting
import com.routeforge.simulation.domain.usecase.ConfirmJoystickInterruptUseCase
import com.routeforge.simulation.domain.usecase.ObserveMockedSessionUseCase
import com.routeforge.simulation.domain.usecase.ObserveRealLocationUseCase
import com.routeforge.simulation.domain.usecase.PauseSimulationUseCase
import com.routeforge.simulation.domain.usecase.RequestJoystickInterruptUseCase
import com.routeforge.simulation.domain.usecase.ResumeSimulationUseCase
import com.routeforge.simulation.domain.usecase.SetSpeedUseCase
import com.routeforge.simulation.domain.usecase.StopSimulationUseCase
import com.routeforge.simulation.domain.usecase.UpdateActiveRouteUseCase
import com.routeforge.simulation.domain.usecase.UpdateJoystickDirectionUseCase
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
    private val pauseSimulationUseCase: PauseSimulationUseCase,
    private val resumeSimulationUseCase: ResumeSimulationUseCase,
    private val stopSimulationUseCase: StopSimulationUseCase,
    observeMockedSessionUseCase: ObserveMockedSessionUseCase,
    private val observeRealLocationUseCase: ObserveRealLocationUseCase,
    private val setSpeedUseCase: SetSpeedUseCase,
    private val requestJoystickInterruptUseCase: RequestJoystickInterruptUseCase,
    private val confirmJoystickInterruptUseCase: ConfirmJoystickInterruptUseCase,
    private val updateJoystickDirectionUseCase: UpdateJoystickDirectionUseCase,
    private val updateActiveRouteUseCase: UpdateActiveRouteUseCase,
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
            is SimulationAction.OnPlaybackSpeedChange -> onPlaybackSpeedChange(action.kmh)
            is SimulationAction.OnExecutionModeSelected -> _state.update { it.copy(executionModeSelection = action.selection) }
            is SimulationAction.OnExecutionTimesInputChange -> _state.update { it.copy(executionTimesInput = action.value) }
            SimulationAction.OnStartRouteSimulation -> _state.update { it.copy(isStartRouteDialogOpen = true) }
            SimulationAction.OnConfirmStartRoute -> routeStarter.confirmStartRoute(_state)
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
            SimulationAction.OnOpenSetupClick ->
                viewModelScope.launch { _events.send(SimulationEvent.NavigateToSetup) }
            SimulationAction.OnLocationPermissionGranted ->
                if (_state.value.mockedSession == null) startObservingRealLocation()
            SimulationAction.OnScreenResumed -> checkAuthorizationStillGranted()
            SimulationAction.OnConfirmFavoriteTeleportCancelRoute -> pendingTeleportConfirmer.confirmFavoriteTeleportCancelRoute(_state)
            SimulationAction.OnDismissFavoriteTeleportCancelRoute ->
                _state.update { it.copy(pendingFavoriteTeleportTarget = null) }
            SimulationAction.OnSwitchRouteModeClick -> onSwitchRouteModeClick()
            SimulationAction.OnConfirmSwitchRouteMode -> confirmSwitchRouteMode()
            SimulationAction.OnDismissSwitchRouteMode -> _state.update { it.copy(pendingRouteModeSwitch = null) }
        }
    }

    /** Applies a committed waypoint edit (spec 008) exactly like [confirmSwitchRouteMode] applies
     *  a mode swap: pushes the recomputed [route] to [lastComputedRouteHolder] (so the loaded-route
     *  UI updates whether or not playback has started) and, if a session is actively playing, to
     *  the controller's live session via [updateActiveRouteUseCase] — a no-op there otherwise. */
    fun applyEditedRoute(route: Route) {
        lastComputedRouteHolder.set(route)
        updateActiveRouteUseCase(route)
    }

    /** Mock-location authorization can be revoked at any time from outside the app (Developer
     *  Options, an MDM policy, the user picking a different mock-location app) — catch that here
     *  instead of only at the next explicit action, and send the user back to setup to fix it. */
    private fun checkAuthorizationStillGranted() {
        if (!mockLocationAuthorizationGuard.isAuthorized()) {
            viewModelScope.launch { _events.send(SimulationEvent.NavigateToSetup) }
        }
    }

    /** Only proposes switching to whichever mode isn't currently active — there are just the two,
     *  so this is a toggle, not a picker. No-op if the loaded route never had both modes
     *  computed (nothing to switch to). */
    private fun onSwitchRouteModeClick() {
        val route = _state.value.loadedRoute ?: return
        if (route.alternateGeometry == null || route.alternateDistanceMeters == null) return
        val targetMode = if (route.mode == RoutePlaybackMode.GUIDED) RoutePlaybackMode.FREE_ROAM else RoutePlaybackMode.GUIDED
        _state.update { it.copy(pendingRouteModeSwitch = targetMode) }
    }

    /** Swaps geometry/mode with their "alternate" counterparts (so switching back and forth keeps
     *  working) and pushes the result both to [lastComputedRouteHolder] (so the loaded-route UI
     *  updates whether or not playback has started yet) and, if a route is actively playing, to
     *  the controller's live session via [updateActiveRouteUseCase] — a no-op there otherwise.
     *  [Route.waypointCumulativeDistances] belongs to whichever geometry was active when it was
     *  computed, so it's cleared on swap (spec 008) — the editable-range check safely treats an
     *  empty list as "unknown, treat everything as editable" until the next edit recomputes it for
     *  the newly active mode. */
    private fun confirmSwitchRouteMode() {
        val targetMode = _state.value.pendingRouteModeSwitch ?: return
        _state.update { it.copy(pendingRouteModeSwitch = null) }
        val route = _state.value.loadedRoute ?: return
        val alternateGeometry = route.alternateGeometry ?: return
        val alternateDistance = route.alternateDistanceMeters ?: return
        val swapped = route.swapToMode(targetMode, alternateGeometry, alternateDistance)
        lastComputedRouteHolder.set(swapped)
        updateActiveRouteUseCase(swapped)
    }

    private fun onPlaybackSpeedChange(kmh: Float) {
        _state.update { it.copy(playbackSpeedKmh = kmh) }
        applyLiveSpeed(SpeedSetting.Manual(kmh / KMH_TO_MPS_DIVISOR))
    }

    /** FR-016: only takes effect immediately if a session is already active; otherwise it's just staged for the next start. */
    private fun applyLiveSpeed(speed: SpeedSetting) {
        if (_state.value.mockedSession != null) setSpeedUseCase(speed)
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
