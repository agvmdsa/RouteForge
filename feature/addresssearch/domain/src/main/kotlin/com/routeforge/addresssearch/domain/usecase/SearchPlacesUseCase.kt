package com.routeforge.addresssearch.domain.usecase

import com.routeforge.addresssearch.domain.GeocodeClient
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.LastKnownRealLocationHolder
import com.routeforge.coredomain.model.PlaceSearchResult

/** Orchestrates a search: calls [GeocodeClient] (which already caps the result count at
 *  [com.routeforge.addresssearch.domain.MAX_SEARCH_RESULTS]), then ranks via [RankSearchResultsUseCase]
 *  using the user's current real location if known, else [fallbackReferenceLatitude]/
 *  [fallbackReferenceLongitude] (the caller's current map center), else no reference point at all
 *  (research.md Decision 5). */
class SearchPlacesUseCase(
    private val geocodeClient: GeocodeClient,
    private val rankSearchResults: RankSearchResultsUseCase,
    private val lastKnownRealLocationHolder: LastKnownRealLocationHolder,
) {
    suspend operator fun invoke(
        query: String,
        fallbackReferenceLatitude: Double?,
        fallbackReferenceLongitude: Double?,
    ): Result<List<PlaceSearchResult>, DataError.Network> =
        when (val result = geocodeClient.search(query)) {
            is Result.Success -> {
                val realLocation = lastKnownRealLocationHolder.location.value
                val referenceLatitude = realLocation?.latitude ?: fallbackReferenceLatitude
                val referenceLongitude = realLocation?.longitude ?: fallbackReferenceLongitude
                Result.Success(rankSearchResults(result.data, referenceLatitude, referenceLongitude))
            }
            is Result.Error -> result
        }
}
