package com.routeforge.routing.presentation.regioncatalog

sealed interface RegionCatalogEvent {
    data class DownloadFailed(
        val message: String,
    ) : RegionCatalogEvent
}
