package com.routeforge.simulation.presentation

import com.routeforge.simulation.domain.model.SpeedSetting
import com.routeforge.simulation.domain.usecase.SetSpeedUseCase
import kotlinx.coroutines.flow.MutableStateFlow

internal const val KMH_TO_MPS_DIVISOR = 3.6f

/** FR-016: only takes effect immediately if a session is already active; otherwise it's just staged for the next start. */
internal fun applyLiveSpeed(
    state: MutableStateFlow<SimulationState>,
    setSpeedUseCase: SetSpeedUseCase,
    speed: SpeedSetting,
) {
    if (state.value.mockedSession != null) setSpeedUseCase(speed)
}
