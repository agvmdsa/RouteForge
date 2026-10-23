package com.routeforge.simulation.presentation

import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.simulation.domain.model.SpeedSetting
import com.routeforge.simulation.domain.usecase.PauseSimulationUseCase
import com.routeforge.simulation.domain.usecase.ResumeSimulationUseCase
import com.routeforge.simulation.domain.usecase.SetSpeedUseCase
import com.routeforge.simulation.domain.usecase.StopSimulationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class PlaybackControls(
    private val pauseSimulationUseCase: PauseSimulationUseCase,
    private val resumeSimulationUseCase: ResumeSimulationUseCase,
    private val stopSimulationUseCase: StopSimulationUseCase,
    private val setSpeedUseCase: SetSpeedUseCase,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
) {
    internal fun pause() = pauseSimulationUseCase()

    internal fun resume() = resumeSimulationUseCase()

    internal fun stop(state: MutableStateFlow<SimulationState>) {
        state.update { it.copy(isPendingCancelRoute = false) }
        stopSimulationUseCase()
        lastComputedRouteHolder.clear()
    }

    internal fun confirmCancelMock(state: MutableStateFlow<SimulationState>) {
        state.update { it.copy(isPendingCancelMock = false) }
        stopSimulationUseCase()
    }

    internal fun onPlaybackSpeedChange(
        kmh: Float,
        state: MutableStateFlow<SimulationState>,
    ) {
        state.update { it.copy(playbackSpeedKmh = kmh) }
        applyLiveSpeed(state, setSpeedUseCase, SpeedSetting.Manual(kmh / KMH_TO_MPS_DIVISOR))
    }
}
