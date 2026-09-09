package com.routeforge.routing.presentation

sealed interface RegionCatalogAction {
    data object OnRefresh : RegionCatalogAction

    data class OnDownloadRegion(
        val regionId: String,
    ) : RegionCatalogAction
}
