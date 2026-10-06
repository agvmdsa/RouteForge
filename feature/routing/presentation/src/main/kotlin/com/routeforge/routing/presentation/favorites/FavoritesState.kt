package com.routeforge.routing.presentation.favorites

import com.routeforge.coredomain.model.FavoriteWaypoint

enum class FavoritesError { INVALID_NAME, INVALID_COORDINATES }

data class FavoritesState(
    val favorites: List<FavoriteWaypoint> = emptyList(),
    val isPickerMode: Boolean = false,
    val editingId: String? = null,
    val editNameInput: String = "",
    val editLatitudeInput: String = "",
    val editLongitudeInput: String = "",
    val editError: FavoritesError? = null,
    val pendingDeleteId: String? = null,
    val pendingTeleportFavorite: FavoriteWaypoint? = null,
    val isPendingTeleportBlockedOffline: Boolean = false,
)
