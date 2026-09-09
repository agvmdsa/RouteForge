package com.routeforge.routing.domain.model

data class Route(
    val points: List<RoutePoint>,
    val geometry: List<Pair<Double, Double>>,
    val distanceMeters: Double,
)
