package com.routeforge.routing.presentation.settings

sealed interface SettingsAction {
    data class OnQuotaSelected(
        val quotaBytes: Long?,
    ) : SettingsAction
}
