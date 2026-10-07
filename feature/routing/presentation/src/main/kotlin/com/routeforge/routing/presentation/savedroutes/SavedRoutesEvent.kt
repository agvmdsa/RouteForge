package com.routeforge.routing.presentation.savedroutes

import com.routeforge.routing.domain.model.RouteFileFormat

sealed interface SavedRoutesEvent {
    data object NavigateBack : SavedRoutesEvent

    data object NavigateToRegionCatalog : SavedRoutesEvent

    class ExportReady(
        val bytes: ByteArray,
        val format: RouteFileFormat,
    ) : SavedRoutesEvent
}
