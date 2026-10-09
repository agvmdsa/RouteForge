package com.routeforge.coredomain.model

/** [alternateGeometry]/[alternateDistanceMeters] hold the *other* [RoutePlaybackMode]'s path for
 *  the same [points], when it was computable — present only when both Guided and Free-roam were
 *  viable at the time this route was finalized, so a caller can offer switching modes later
 *  (even mid-playback) without recomputing anything. Null when only one mode was ever viable.
 *
 *  [waypointCumulativeDistances], when populated, gives the distance in meters along [geometry]
 *  (from `geometry.first()`) to reach each entry in [points] at the same index — used to compute
 *  which waypoints a simulation has already passed (spec 008). Defaults to an empty list for
 *  routes built outside this feature's own producers ([FreeRoamRouteBuilder], the Guided routing
 *  engine) — callers must treat an empty list the same as "not yet known," never as "zero
 *  waypoints are editable." */
data class Route(
    val points: List<RoutePoint>,
    val geometry: List<Pair<Double, Double>>,
    val distanceMeters: Double,
    val mode: RoutePlaybackMode = RoutePlaybackMode.GUIDED,
    val alternateGeometry: List<Pair<Double, Double>>? = null,
    val alternateDistanceMeters: Double? = null,
    val waypointCumulativeDistances: List<Double> = emptyList(),
)
