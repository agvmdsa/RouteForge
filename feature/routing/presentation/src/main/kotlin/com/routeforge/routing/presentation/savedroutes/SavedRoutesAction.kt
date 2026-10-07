package com.routeforge.routing.presentation.savedroutes

import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.routing.domain.model.RouteFileFormat

sealed interface SavedRoutesAction {
    data class OnEditClick(
        val id: String,
    ) : SavedRoutesAction

    data class OnEditNameChange(
        val value: String,
    ) : SavedRoutesAction

    data object OnConfirmEdit : SavedRoutesAction

    data object OnDismissEdit : SavedRoutesAction

    data class OnDeleteClick(
        val id: String,
    ) : SavedRoutesAction

    data object OnConfirmDelete : SavedRoutesAction

    data object OnDismissDelete : SavedRoutesAction

    data class OnUseClick(
        val id: String,
    ) : SavedRoutesAction

    data class OnChooseMode(
        val mode: RoutePlaybackMode,
    ) : SavedRoutesAction

    data object OnDismissModeChoice : SavedRoutesAction

    data object OnOpenRegionCatalog : SavedRoutesAction

    data object OnProceedDespiteMissingRegions : SavedRoutesAction

    data object OnDismissMissingRegionsWarning : SavedRoutesAction

    data class OnExportClick(
        val id: String,
    ) : SavedRoutesAction

    data class OnChooseExportFormat(
        val format: RouteFileFormat,
    ) : SavedRoutesAction

    data object OnDismissExportFormat : SavedRoutesAction
}
