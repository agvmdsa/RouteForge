package com.routeforge.routing.presentation.favorites

internal sealed interface FavoritesEvent {
    data object NavigateBack : FavoritesEvent
}
