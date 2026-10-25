package com.routeforge.mocklocationsetup.presentation

import com.routeforge.mocklocationsetup.domain.model.SetupShortcut

internal sealed interface MockLocationSetupEvent {
    data class LaunchDeepLink(
        val shortcut: SetupShortcut,
    ) : MockLocationSetupEvent
}
