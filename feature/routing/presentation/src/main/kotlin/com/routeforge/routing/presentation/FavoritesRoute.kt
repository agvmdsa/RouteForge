package com.routeforge.routing.presentation

import kotlinx.serialization.Serializable

@Serializable
data class FavoritesRoute(
    val isPickerMode: Boolean = false,
)
