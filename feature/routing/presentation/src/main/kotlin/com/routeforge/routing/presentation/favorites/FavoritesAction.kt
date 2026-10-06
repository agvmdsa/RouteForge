package com.routeforge.routing.presentation.favorites

sealed interface FavoritesAction {
    data class OnFavoriteSelected(
        val id: String,
    ) : FavoritesAction

    data class OnEditClick(
        val id: String,
    ) : FavoritesAction

    data class OnEditNameChange(
        val value: String,
    ) : FavoritesAction

    data class OnEditLatitudeChange(
        val value: String,
    ) : FavoritesAction

    data class OnEditLongitudeChange(
        val value: String,
    ) : FavoritesAction

    data object OnConfirmEdit : FavoritesAction

    data object OnDismissEdit : FavoritesAction

    data class OnDeleteClick(
        val id: String,
    ) : FavoritesAction

    data object OnConfirmDelete : FavoritesAction

    data object OnDismissDelete : FavoritesAction

    data class OnMockHereClick(
        val id: String,
    ) : FavoritesAction

    data object OnConfirmMockHere : FavoritesAction

    data object OnDismissMockHere : FavoritesAction
}
