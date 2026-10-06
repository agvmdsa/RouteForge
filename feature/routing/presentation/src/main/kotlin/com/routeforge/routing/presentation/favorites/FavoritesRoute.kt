package com.routeforge.routing.presentation.favorites

import kotlinx.serialization.Serializable

@Serializable
data class FavoritesRoute(
    val isPickerMode: Boolean = false,
)
