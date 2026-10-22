package com.routeforge.app

import kotlinx.serialization.Serializable

/** Marks the 4 bottom-tab destinations, so [MainTabsScaffold]/[switchToTab] can take one typed
 *  argument instead of `Any`. Not itself [Serializable] — Navigation-Compose resolves
 *  serialization off each object's own runtime class, not this marker. */
internal sealed interface TabRoute

@Serializable internal data object SimulateTabRoute : TabRoute

@Serializable internal data object PlanTabRoute : TabRoute

@Serializable internal data object SavedTabRoute : TabRoute

@Serializable internal data object SettingsTabRoute : TabRoute
