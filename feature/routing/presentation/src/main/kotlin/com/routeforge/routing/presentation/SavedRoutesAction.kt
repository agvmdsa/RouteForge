package com.routeforge.routing.presentation

import com.routeforge.coredomain.model.RoutePlaybackMode

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

    data object OnOpenRegionCatalog : SavedRoutesAction

    data object OnProceedDespiteMissingRegions : SavedRoutesAction

    data object OnDismissMissingRegionsWarning : SavedRoutesAction
}
