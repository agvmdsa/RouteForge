package com.routeforge.routing.presentation.settings

import com.routeforge.coredomain.model.MapStyle

sealed interface SettingsAction {
    data class OnQuotaSelected(
        val quotaBytes: Long?,
    ) : SettingsAction

    data class OnMapStyleSelected(
        val mapStyle: MapStyle,
    ) : SettingsAction
}
