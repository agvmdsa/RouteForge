package com.routeforge.simulation.presentation.waypointedit

import com.routeforge.coredomain.FavoriteWaypointsRepository
import com.routeforge.coredomain.model.FavoriteWaypoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeFavoriteWaypointsRepository : FavoriteWaypointsRepository {
    private val _favorites = MutableStateFlow<List<FavoriteWaypoint>>(emptyList())
    private var nextId = 0

    override fun observeFavorites(): StateFlow<List<FavoriteWaypoint>> = _favorites

    override fun add(
        name: String,
        latitude: Double,
        longitude: Double,
    ): FavoriteWaypoint {
        val favorite = FavoriteWaypoint(id = "fake-${nextId++}", name = name, latitude = latitude, longitude = longitude)
        _favorites.value = _favorites.value + favorite
        return favorite
    }

    override fun update(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double,
    ) {
        _favorites.value =
            _favorites.value.map { favorite ->
                if (favorite.id == id) favorite.copy(name = name, latitude = latitude, longitude = longitude) else favorite
            }
    }

    override fun delete(id: String) {
        _favorites.value = _favorites.value.filterNot { it.id == id }
    }
}
