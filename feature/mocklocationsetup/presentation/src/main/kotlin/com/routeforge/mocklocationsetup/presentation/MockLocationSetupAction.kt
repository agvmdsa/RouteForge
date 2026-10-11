package com.routeforge.mocklocationsetup.presentation

import android.content.Intent

sealed interface MockLocationSetupAction {
    data class OnDeepLinkClick(
        val intent: Intent,
    ) : MockLocationSetupAction
}
