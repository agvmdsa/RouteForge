package com.routeforge.coredomain.model

/** A single geocoded candidate returned by a place/address search — one row in a results list.
 *  [importance] is the geocoding service's own precomputed, static per-place significance score
 *  (not query-dependent), used alongside distance-to-reference-point for ranking. */
data class PlaceSearchResult(
    val name: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val importance: Double,
)
