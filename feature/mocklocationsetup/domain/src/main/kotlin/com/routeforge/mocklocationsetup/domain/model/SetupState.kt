package com.routeforge.mocklocationsetup.domain.model

data class SetupState(
    val pendingSteps: List<SetupStep>,
    val isBlockedByPolicy: Boolean,
) {
    val isReady: Boolean get() = pendingSteps.isEmpty() && !isBlockedByPolicy
}
