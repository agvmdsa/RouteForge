package com.routeforge.mocklocationsetup.presentation

import android.content.Intent
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
    val deepLinkIntent: Intent?,
)
