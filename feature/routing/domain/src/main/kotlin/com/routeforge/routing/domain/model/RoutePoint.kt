package com.routeforge.routing.domain.model

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val snappedLatitude: Double? = null,
    val snappedLongitude: Double? = null,
)
