package com.routeforge.mocklocationsetup.presentation

import com.routeforge.mocklocationsetup.domain.model.SetupStepId

data class MockLocationSetupState(
    val pendingSteps: List<SetupStepUi> = emptyList(),
    val isReady: Boolean = false,
    val isBlockedByPolicy: Boolean = false,
)

data class SetupStepUi(
    val id: SetupStepId,
    val title: String,
    val explanation: String,
    val instructions: String,
    val canDeepLink: Boolean,
)
