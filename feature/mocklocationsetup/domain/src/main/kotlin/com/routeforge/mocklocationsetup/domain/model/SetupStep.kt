package com.routeforge.mocklocationsetup.domain.model

data class SetupStep(
    val id: SetupStepId,
    val title: String,
    val explanation: String,
    val instructions: String,
    val canDeepLink: Boolean,
)
