package com.routeforge.routing.presentation

sealed interface FavoritesEvent {
    data object NavigateBack : FavoritesEvent
}
