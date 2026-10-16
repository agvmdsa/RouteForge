package com.routeforge.routing.presentation.settings

/** Tab-switch requests Settings can't perform itself — a [com.routeforge.coredomain.model.TourSection]'s
 *  tab lives in the app module's own navigation graph, which this feature module must not depend
 *  on, so [SettingsRoot] turns each event into whatever tab-switch mechanism the host app uses. */
sealed interface SettingsEvent {
    data object NavigateToSimulationTab : SettingsEvent

    data object NavigateToRouteCreationTab : SettingsEvent
}
