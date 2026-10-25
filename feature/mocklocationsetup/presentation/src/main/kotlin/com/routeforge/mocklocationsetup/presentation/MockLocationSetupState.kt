package com.routeforge.mocklocationsetup.presentation

import com.routeforge.mocklocationsetup.domain.model.SetupShortcut
import com.routeforge.mocklocationsetup.domain.model.SetupStepId

internal data class MockLocationSetupState(
    val pendingSteps: List<SetupStepUi> = emptyList(),
    val isReady: Boolean = false,
    val isBlockedByPolicy: Boolean = false,
)

internal data class SetupStepUi(
    val id: SetupStepId,
    val title: String,
    val explanation: String,
    val instructions: String,
    val shortcut: SetupShortcut?,
)
