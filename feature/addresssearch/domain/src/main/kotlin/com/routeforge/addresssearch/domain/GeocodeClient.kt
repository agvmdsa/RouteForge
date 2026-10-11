package com.routeforge.addresssearch.domain

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.PlaceSearchResult

/** The maximum number of candidates any [GeocodeClient] implementation should request from its
 *  backing service — also the ceiling [com.routeforge.addresssearch.domain.usecase.SearchPlacesUseCase]
 *  ranks within, so the fetch limit and the ranking cap can't silently drift apart into two
 *  independently-guessed numbers. */
const val MAX_SEARCH_RESULTS = 10

/** Geocodes a free-text query into a list of candidate places, capped at [MAX_SEARCH_RESULTS].
 *  Implemented in `feature:addresssearch:data` against the public Nominatim API. */
interface GeocodeClient {
    suspend fun search(query: String): Result<List<PlaceSearchResult>, DataError.Network>
}
