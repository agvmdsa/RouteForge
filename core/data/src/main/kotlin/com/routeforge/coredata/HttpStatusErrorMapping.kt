package com.routeforge.coredata

import com.routeforge.coredomain.DataError
import io.ktor.http.HttpStatusCode

/** Shared HTTP status → [DataError.Network] mapping for every Ktor-backed data source in the app,
 *  so a new client can't silently diverge from an existing one's classification the way
 *  `HttpRegionDownloader` and `NominatimGeocodeClient` once independently did. */
fun HttpStatusCode.toNetworkError(): DataError.Network =
    when (value) {
        401 -> DataError.Network.UNAUTHORIZED
        403 -> DataError.Network.FORBIDDEN
        404 -> DataError.Network.NOT_FOUND
        408 -> DataError.Network.REQUEST_TIMEOUT
        409 -> DataError.Network.CONFLICT
        413 -> DataError.Network.PAYLOAD_TOO_LARGE
        429 -> DataError.Network.TOO_MANY_REQUESTS
        in 500..599 -> DataError.Network.SERVER_ERROR
        else -> DataError.Network.UNKNOWN
    }
