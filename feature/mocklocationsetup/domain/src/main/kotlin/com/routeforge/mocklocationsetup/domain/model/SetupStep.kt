package com.routeforge.mocklocationsetup.domain.model

import android.content.Intent

data class SetupStep(
    val id: SetupStepId,
    val title: String,
    val explanation: String,
    val instructions: String,
    val deepLinkIntent: Intent?,
)
