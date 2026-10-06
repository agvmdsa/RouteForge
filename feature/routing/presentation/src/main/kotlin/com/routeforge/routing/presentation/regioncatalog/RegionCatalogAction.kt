package com.routeforge.routing.presentation.regioncatalog

sealed interface RegionCatalogAction {
    data object OnRefresh : RegionCatalogAction

    data class OnDownloadRegion(
        val regionId: String,
    ) : RegionCatalogAction

    data object OnCancelDownloadClick : RegionCatalogAction

    data object OnConfirmCancelDownload : RegionCatalogAction

    data object OnDismissCancelDownload : RegionCatalogAction

    data class OnDeleteRegionClick(
        val regionId: String,
    ) : RegionCatalogAction

    data object OnConfirmDeleteRegion : RegionCatalogAction

    data object OnDismissDeleteRegion : RegionCatalogAction
}
