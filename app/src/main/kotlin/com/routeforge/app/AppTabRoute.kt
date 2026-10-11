package com.routeforge.app

import kotlinx.serialization.Serializable

/** Marks the 4 bottom-tab destinations, so [MainTabsScaffold]/[switchToTab] can take one typed
 *  argument instead of `Any`. Not itself [Serializable] — Navigation-Compose resolves
 *  serialization off each object's own runtime class, not this marker. */
sealed interface TabRoute

@Serializable data object SimulateTabRoute : TabRoute

@Serializable data object PlanTabRoute : TabRoute

@Serializable data object SavedTabRoute : TabRoute

@Serializable data object SettingsTabRoute : TabRoute
