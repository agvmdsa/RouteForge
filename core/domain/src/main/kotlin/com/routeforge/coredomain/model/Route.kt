package com.routeforge.coredomain.model

/** [alternateGeometry]/[alternateDistanceMeters] hold the *other* [RoutePlaybackMode]'s path for
 *  the same [points], when it was computable — present only when both Guided and Free-roam were
 *  viable at the time this route was finalized, so a caller can offer switching modes later
 *  (even mid-playback) without recomputing anything. Null when only one mode was ever viable. */
data class Route(
    val points: List<RoutePoint>,
    val geometry: List<Pair<Double, Double>>,
    val distanceMeters: Double,
    val mode: RoutePlaybackMode = RoutePlaybackMode.GUIDED,
    val alternateGeometry: List<Pair<Double, Double>>? = null,
    val alternateDistanceMeters: Double? = null,
)
