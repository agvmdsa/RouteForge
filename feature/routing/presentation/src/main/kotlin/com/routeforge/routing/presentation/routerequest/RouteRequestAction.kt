package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.routing.domain.model.RouteFileFormat

sealed interface RouteRequestAction {
    data class OnMapTap(
        val latitude: Double,
        val longitude: Double,
    ) : RouteRequestAction

    data class OnMarkerDragged(
        val index: Int,
        val latitude: Double,
        val longitude: Double,
    ) : RouteRequestAction

    data class OnMarkerClick(
        val index: Int,
    ) : RouteRequestAction

    data class OnEditLatitudeChange(
        val value: String,
    ) : RouteRequestAction

    data class OnEditLongitudeChange(
        val value: String,
    ) : RouteRequestAction

    data object OnConfirmEdit : RouteRequestAction

    data object OnDeleteEditingWaypoint : RouteRequestAction

    data object OnDismissEdit : RouteRequestAction

    data object OnUndo : RouteRequestAction

    data object OnConfirmAddWaypoint : RouteRequestAction

    data object OnDismissAddWaypoint : RouteRequestAction

    data object OnToggleSaveAsFavorite : RouteRequestAction

    data class OnFavoriteNameInputChange(
        val value: String,
    ) : RouteRequestAction

    data object OnOpenFavoritesClick : RouteRequestAction

    data object OnSaveRouteClick : RouteRequestAction

    data class OnRouteNameInputChange(
        val value: String,
    ) : RouteRequestAction

    data object OnConfirmSaveRoute : RouteRequestAction

    data object OnDismissSaveRoute : RouteRequestAction

    class OnRouteFileImported(
        val bytes: ByteArray,
        val format: RouteFileFormat,
    ) : RouteRequestAction

    data class OnExportRoute(
        val format: RouteFileFormat,
    ) : RouteRequestAction

    data object OnRequestRoute : RouteRequestAction

    data class OnChooseMode(
        val mode: RoutePlaybackMode,
    ) : RouteRequestAction

    data object OnDismissModeChoice : RouteRequestAction

    data object OnUseRoute : RouteRequestAction

    data object OnOpenRegionCatalog : RouteRequestAction

    data object OnProceedDespiteMissingRegions : RouteRequestAction

    data object OnDismissMissingRegionsWarning : RouteRequestAction

    data object OnConfirmGoToSimulate : RouteRequestAction

    data object OnDismissGoToSimulateConfirmation : RouteRequestAction

    data object OnSwitchRouteModeClick : RouteRequestAction
}
