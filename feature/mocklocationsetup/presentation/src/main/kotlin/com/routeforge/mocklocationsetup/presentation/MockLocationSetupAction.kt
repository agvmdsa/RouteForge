package com.routeforge.mocklocationsetup.presentation

import com.routeforge.mocklocationsetup.domain.model.SetupShortcut

internal sealed interface MockLocationSetupAction {
    data class OnDeepLinkClick(
        val shortcut: SetupShortcut,
    ) : MockLocationSetupAction
}
