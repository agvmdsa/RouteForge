package com.routeforge.coredomain.model

data class FavoriteRoute(
    val id: String,
    val name: String,
    val points: List<RoutePoint>,
)
