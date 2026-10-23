package com.routeforge.simulation.presentation

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.simulation.domain.model.JoystickInterruptDecision
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SimulationStatus
import com.routeforge.simulation.domain.model.SpeedSetting
import com.routeforge.simulation.domain.usecase.ConfirmJoystickInterruptUseCase
import com.routeforge.simulation.domain.usecase.PauseSimulationUseCase
import com.routeforge.simulation.domain.usecase.RequestJoystickInterruptUseCase
import com.routeforge.simulation.domain.usecase.ResumeSimulationUseCase
import com.routeforge.simulation.domain.usecase.SetSpeedUseCase
import com.routeforge.simulation.domain.usecase.StopSimulationUseCase
import com.routeforge.simulation.domain.usecase.UpdateJoystickDirectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class JoystickSessionController(
    private val requestJoystickInterruptUseCase: RequestJoystickInterruptUseCase,
    private val confirmJoystickInterruptUseCase: ConfirmJoystickInterruptUseCase,
    private val updateJoystickDirectionUseCase: UpdateJoystickDirectionUseCase,
    private val pauseSimulationUseCase: PauseSimulationUseCase,
    private val resumeSimulationUseCase: ResumeSimulationUseCase,
    private val stopSimulationUseCase: StopSimulationUseCase,
    private val setSpeedUseCase: SetSpeedUseCase,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
) {
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

    internal fun onMockedSessionChanged(mockedSession: SimulationSession?) {
        if (mockedSession?.mode != SimulationMode.JOYSTICK) {
            joystickSessionStarted = false
        }
    }

    /**
     * FR-022/FR-024–FR-026 (revised): the joystick's visibility is an explicit toggle, separate
     * from whether it is actually mocking a position yet (that only starts once the user drags —
     * see [onJoystickDrag]). Turning it on while a route is loaded or actively playing requires
     * confirmation, because confirming clears the loaded route entirely. Turning it off while it
     * was actively mocking fully stops the session, returning to the real location.
     */
    internal fun onJoystickDrag(
        bearingDegrees: Float,
        state: MutableStateFlow<SimulationState>,
    ) {
        if (!joystickSessionStarted) {
            when (confirmJoystickInterruptUseCase()) {
                is Result.Success -> {
                    joystickSessionStarted = true
                    state.update { it.copy(errorType = null) }
                    setSpeedUseCase(SpeedSetting.Manual(state.value.joystickSpeedKmh / KMH_TO_MPS_DIVISOR))
                }
                is Result.Error -> {
                    state.update { it.copy(errorType = SimulationErrorType.JOYSTICK_NO_REAL_FIX) }
                    return
                }
            }
        } else if (state.value.mockedSession?.status == SimulationStatus.PAUSED) {
            // A prior drag gesture ended (which pauses in place, per FR "releasing stops in
            // place"); a *new* drag gesture on an already-started session must resume it, or
            // every drag after the first release would silently update direction with the
            // session still paused and never actually move.
            resumeSimulationUseCase()
        }
        updateJoystickDirectionUseCase(bearingDegrees)
    }

    internal fun onJoystickReleased() = pauseSimulationUseCase()

    internal fun onToggleJoystick(state: MutableStateFlow<SimulationState>) {
        if (state.value.isJoystickVisible) {
            if (state.value.mockedSession?.mode == SimulationMode.JOYSTICK) {
                stopSimulationUseCase()
            }
            joystickSessionStarted = false
            state.update { it.copy(isJoystickVisible = false) }
            return
        }

        val routeIsLoadedOrRunning =
            state.value.loadedRoute != null ||
                requestJoystickInterruptUseCase() == JoystickInterruptDecision.ConfirmationRequired
        if (routeIsLoadedOrRunning) {
            state.update { it.copy(isJoystickInterruptPending = true) }
        } else {
            state.update { it.copy(isJoystickVisible = true) }
        }
    }

    /** The joystick's own speed control, always available while it's visible — no need to load a route. */
    internal fun onJoystickSpeedChange(
        kmh: Float,
        state: MutableStateFlow<SimulationState>,
    ) {
        state.update { it.copy(joystickSpeedKmh = kmh) }
        applyLiveSpeed(state, setSpeedUseCase, SpeedSetting.Manual(kmh / KMH_TO_MPS_DIVISOR))
    }

    internal fun confirmJoystickInterrupt(state: MutableStateFlow<SimulationState>) {
        lastComputedRouteHolder.clear()
        stopSimulationUseCase()
        joystickSessionStarted = false
        state.update { it.copy(isJoystickInterruptPending = false, isJoystickVisible = true) }
    }
}
