package com.routeforge.routing.presentation.settings

import com.routeforge.coredomain.model.MapStyle
import com.routeforge.coredomain.model.TourSection

internal sealed interface SettingsAction {
    data class OnQuotaSelected(
        val quotaBytes: Long?,
    ) : SettingsAction

    data class OnMapStyleSelected(
        val mapStyle: MapStyle,
    ) : SettingsAction

    data class OnRepeatTourClick(
        val section: TourSection,
    ) : SettingsAction

    data object OnRepeatAllToursClick : SettingsAction
}
