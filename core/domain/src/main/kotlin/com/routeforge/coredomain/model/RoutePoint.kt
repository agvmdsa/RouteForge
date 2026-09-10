package com.routeforge.coredomain.model

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val snappedLatitude: Double? = null,
    val snappedLongitude: Double? = null,
)
