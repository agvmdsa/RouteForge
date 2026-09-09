package com.routeforge.routing.presentation

sealed interface RegionCatalogEvent {
    data class DownloadFailed(
        val message: String,
    ) : RegionCatalogEvent
}
