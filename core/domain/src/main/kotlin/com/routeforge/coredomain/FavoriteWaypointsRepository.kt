package com.routeforge.coredomain

import com.routeforge.coredomain.model.FavoriteWaypoint
import kotlinx.coroutines.flow.StateFlow

interface FavoriteWaypointsRepository {
    fun observeFavorites(): StateFlow<List<FavoriteWaypoint>>

    /** Generates the id; returns the created favorite. */
    fun add(
        name: String,
        latitude: Double,
        longitude: Double,
    ): FavoriteWaypoint

    fun update(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double,
    )

    fun delete(id: String)
}
