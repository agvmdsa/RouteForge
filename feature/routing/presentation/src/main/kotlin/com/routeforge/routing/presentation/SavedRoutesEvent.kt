package com.routeforge.routing.presentation

sealed interface SavedRoutesEvent {
    data object NavigateBack : SavedRoutesEvent

    data object NavigateToRegionCatalog : SavedRoutesEvent
}
