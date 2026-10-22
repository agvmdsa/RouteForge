package com.routeforge.routing.presentation.regioncatalog

internal sealed interface RegionCatalogEvent {
    data class DownloadFailed(
        val message: String,
    ) : RegionCatalogEvent
}
