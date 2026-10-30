package com.routeforge.coredomain.model

/** A place and what it is called, when it is called anything. */
data class NamedPlace(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
)
