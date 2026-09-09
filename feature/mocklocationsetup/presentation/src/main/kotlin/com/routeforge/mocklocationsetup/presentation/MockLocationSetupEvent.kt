package com.routeforge.mocklocationsetup.presentation

import android.content.Intent

sealed interface MockLocationSetupEvent {
    data class LaunchDeepLink(
        val intent: Intent,
    ) : MockLocationSetupEvent
}
