package com.routeforge.addresssearch.domain.testing

import com.routeforge.addresssearch.domain.GeocodeClient
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.PlaceSearchResult
import kotlinx.coroutines.CompletableDeferred

class FakeGeocodeClient(
    var result: Result<List<PlaceSearchResult>, DataError.Network> = Result.Success(emptyList()),
) : GeocodeClient {
    /** Queued responses that only resolve when the test completes them — lets a test hold a
     *  search suspended mid-flight instead of [search] returning [result] immediately. Falls back
     *  to [result] once empty, so every other test (which never touches this) is unaffected. */
    val queuedResponses = mutableListOf<CompletableDeferred<Result<List<PlaceSearchResult>, DataError.Network>>>()

    /** Every query that was asked for, in order. */
    val queries = mutableListOf<String>()

    /** The query of the latest search. */
    var lastQuery: String? = null

    override suspend fun search(query: String): Result<List<PlaceSearchResult>, DataError.Network> {
        lastQuery = query
        queries += query
        return if (queuedResponses.isNotEmpty()) queuedResponses.removeAt(0).await() else result
    }
}
