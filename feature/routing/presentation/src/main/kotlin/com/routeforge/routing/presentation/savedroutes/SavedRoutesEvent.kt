package com.routeforge.routing.presentation.savedroutes

sealed interface SavedRoutesEvent {
    data object NavigateBack : SavedRoutesEvent

    data object NavigateToRegionCatalog : SavedRoutesEvent
}
