package com.routeforge.routing.presentation

sealed interface SettingsAction {
    data class OnQuotaSelected(
        val quotaBytes: Long?,
    ) : SettingsAction
}
