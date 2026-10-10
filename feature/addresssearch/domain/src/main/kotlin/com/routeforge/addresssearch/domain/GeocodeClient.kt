package com.routeforge.addresssearch.domain

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.PlaceSearchResult

/** Geocodes a free-text query into a list of candidate places. Implemented in
 *  `feature:addresssearch:data` against the public Nominatim API. */
interface GeocodeClient {
    suspend fun search(query: String): Result<List<PlaceSearchResult>, DataError.Network>
}
