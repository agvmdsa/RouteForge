package com.routeforge.routing.domain

import com.routeforge.coredomain.FavoriteRoutesRepository
import com.routeforge.coredomain.model.FavoriteRoute
import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeFavoriteRoutesRepository(
    initialFavoriteRoutes: List<FavoriteRoute> = emptyList(),
) : FavoriteRoutesRepository {
    private val favoriteRoutesState = MutableStateFlow(initialFavoriteRoutes)
    private var nextId = 0

    override fun observeFavoriteRoutes(): StateFlow<List<FavoriteRoute>> = favoriteRoutesState

    override fun add(
        name: String,
        points: List<RoutePoint>,
    ): FavoriteRoute {
        val favoriteRoute = FavoriteRoute(id = "fake-${nextId++}", name = name, points = points)
        favoriteRoutesState.value = favoriteRoutesState.value + favoriteRoute
        return favoriteRoute
    }

    override fun rename(
        id: String,
        name: String,
    ) {
        favoriteRoutesState.value =
            favoriteRoutesState.value.map { favoriteRoute ->
                if (favoriteRoute.id == id) favoriteRoute.copy(name = name) else favoriteRoute
            }
    }

    override fun delete(id: String) {
        favoriteRoutesState.value = favoriteRoutesState.value.filterNot { it.id == id }
    }
}
