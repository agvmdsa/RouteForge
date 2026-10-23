package com.routeforge.simulation.presentation

import com.routeforge.simulation.domain.model.ExecutionMode
import com.routeforge.simulation.domain.model.SpeedSetting
import com.routeforge.simulation.domain.usecase.StartRouteSimulationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class RouteStarter(
    private val startRouteSimulationUseCase: StartRouteSimulationUseCase,
    private val mockLocationAuthorizationGuard: MockLocationAuthorizationGuard,
) {
    /** Called from the "how many times" dialog's confirm button — validates and, if everything
     *  checks out, actually starts playback and closes the dialog. An invalid times count keeps
     *  the dialog open (the fix belongs right there); an authorization failure closes it, since
     *  that's resolved elsewhere (the setup screen). */
    internal fun confirmStartRoute(state: MutableStateFlow<SimulationState>) {
        val route = state.value.loadedRoute ?: return
        val speedKmh = state.value.playbackSpeedKmh
        if (speedKmh <= 0f) {
            state.update { it.copy(isStartRouteDialogOpen = false, errorType = SimulationErrorType.INVALID_SPEED) }
            return
        }
        val executionMode = resolveExecutionMode(state.value.executionModeSelection, state.value.executionTimesInput)
        if (executionMode == null) {
            state.update { it.copy(errorType = SimulationErrorType.INVALID_EXECUTION_TIMES) }
            return
        }

        if (!mockLocationAuthorizationGuard.isAuthorized()) {
            state.update { it.copy(isStartRouteDialogOpen = false) }
            mockLocationAuthorizationGuard.reportUnauthorized(state)
            return
        }
        state.update { it.copy(isStartRouteDialogOpen = false, errorType = null, isBlockedByAuthorization = false) }
        startRouteSimulationUseCase(route, SpeedSetting.Manual(speedKmh / KMH_TO_MPS_DIVISOR), executionMode)
    }
}

internal fun resolveExecutionMode(
    selection: ExecutionModeSelection,
    timesInput: String,
): ExecutionMode? =
    when (selection) {
        ExecutionModeSelection.ONCE -> ExecutionMode.Once
        ExecutionModeSelection.LOOP -> ExecutionMode.Loop
        ExecutionModeSelection.PING_PONG -> ExecutionMode.PingPong
        ExecutionModeSelection.TIMES -> {
            val count = timesInput.toIntOrNull()
            if (count == null || count <= 0) null else ExecutionMode.Times(count)
        }
    }
