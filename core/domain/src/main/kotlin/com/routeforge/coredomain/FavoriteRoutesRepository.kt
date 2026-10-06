package com.routeforge.coredomain

import com.routeforge.coredomain.model.FavoriteRoute
import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.StateFlow

interface FavoriteRoutesRepository {
    fun observeFavoriteRoutes(): StateFlow<List<FavoriteRoute>>

    /** Generates the id; returns the created favorite route. */
    fun add(
        name: String,
        points: List<RoutePoint>,
    ): FavoriteRoute

    fun rename(
        id: String,
        name: String,
    )

    fun delete(id: String)
}
