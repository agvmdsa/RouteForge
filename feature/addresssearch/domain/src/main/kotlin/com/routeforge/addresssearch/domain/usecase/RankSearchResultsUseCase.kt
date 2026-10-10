package com.routeforge.addresssearch.domain.usecase

import com.routeforge.coredomain.GeoMath
import com.routeforge.coredomain.model.PlaceSearchResult

private const val IMPORTANCE_WEIGHT = 0.5
private const val PROXIMITY_WEIGHT = 0.5
private const val PROXIMITY_DECAY_METERS = 1_000.0

/** Orders search results by a composite score combining each place's own [PlaceSearchResult.importance]
 *  (a static, precomputed significance rank) with its proximity to a reference point — not a plain
 *  pass-through of whatever order the geocoding service returned (spec FR-008). With no reference point
 *  at all (both arguments null), falls back to importance alone. */
class RankSearchResultsUseCase {
    operator fun invoke(
        results: List<PlaceSearchResult>,
        referenceLatitude: Double?,
        referenceLongitude: Double?,
    ): List<PlaceSearchResult> {
        if (referenceLatitude == null || referenceLongitude == null) {
            return results.sortedByDescending { it.importance }
        }
        return results.sortedByDescending { score(it, referenceLatitude, referenceLongitude) }
    }

    private fun score(
        result: PlaceSearchResult,
        referenceLatitude: Double,
        referenceLongitude: Double,
    ): Double {
        val distanceMeters = GeoMath.haversineMeters(referenceLatitude, referenceLongitude, result.latitude, result.longitude)
        val proximityScore = 1.0 / (1.0 + distanceMeters / PROXIMITY_DECAY_METERS)
        return result.importance * IMPORTANCE_WEIGHT + proximityScore * PROXIMITY_WEIGHT
    }
}
