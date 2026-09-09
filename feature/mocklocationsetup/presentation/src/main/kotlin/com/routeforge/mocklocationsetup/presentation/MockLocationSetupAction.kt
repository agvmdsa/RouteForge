package com.routeforge.mocklocationsetup.presentation

import com.routeforge.mocklocationsetup.domain.model.SetupStepId

sealed interface MockLocationSetupAction {
    data class OnDeepLinkClick(
        val stepId: SetupStepId,
    ) : MockLocationSetupAction
}
