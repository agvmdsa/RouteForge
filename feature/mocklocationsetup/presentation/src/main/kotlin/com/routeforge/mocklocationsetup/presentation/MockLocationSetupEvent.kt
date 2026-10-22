package com.routeforge.mocklocationsetup.presentation

import android.content.Intent

internal sealed interface MockLocationSetupEvent {
    data class LaunchDeepLink(
        val intent: Intent,
    ) : MockLocationSetupEvent
}
