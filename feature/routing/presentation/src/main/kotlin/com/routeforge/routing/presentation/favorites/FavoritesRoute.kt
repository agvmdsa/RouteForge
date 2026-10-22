package com.routeforge.routing.presentation.favorites

import kotlinx.serialization.Serializable

@Serializable
internal data class FavoritesRoute(
    val isPickerMode: Boolean = false,
)
