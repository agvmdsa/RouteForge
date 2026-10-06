package com.routeforge.routing.presentation.favorites

sealed interface FavoritesEvent {
    data object NavigateBack : FavoritesEvent
}
