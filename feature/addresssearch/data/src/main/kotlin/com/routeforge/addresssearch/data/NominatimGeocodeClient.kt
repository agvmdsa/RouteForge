package com.routeforge.addresssearch.data

import android.util.Log
import com.routeforge.addresssearch.domain.GeocodeClient
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.PlaceSearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.UnknownHostException

private const val TAG = "NominatimGeocodeClient"
private const val SEARCH_URL = "https://nominatim.openstreetmap.org/search"
private const val REQUEST_LIMIT = 10

class NominatimGeocodeClient(
    private val httpClient: HttpClient,
) : GeocodeClient {
    override suspend fun search(query: String): Result<List<PlaceSearchResult>, DataError.Network> =
        try {
            val response =
                httpClient.get(SEARCH_URL) {
                    parameter("q", query)
                    parameter("format", "jsonv2")
                    parameter("limit", REQUEST_LIMIT)
                }
            if (!response.status.isSuccess()) {
                Log.w(TAG, "Nominatim returned ${response.status}")
                Result.Error(response.status.toNetworkError())
            } else {
                val dtos = response.body<List<NominatimResultDto>>()
                Result.Success(dtos.mapNotNull { it.toPlaceSearchResult() })
            }
        } catch (e: HttpRequestTimeoutException) {
            // Must be caught before CancellationException below — Ktor's HttpTimeout plugin throws
            // a subclass of it, which would otherwise be silently rethrown as a plain cancellation
            // instead of surfacing as a visible, accurate error.
            Log.w(TAG, "Search request timed out", e)
            Result.Error(DataError.Network.REQUEST_TIMEOUT)
        } catch (e: SerializationException) {
            Log.w(TAG, "Couldn't parse Nominatim's response", e)
            Result.Error(DataError.Network.SERIALIZATION)
        } catch (e: JsonConvertException) {
            // Ktor's own wrapper around a failed body conversion — does NOT extend
            // kotlinx.serialization.SerializationException, so it needs its own catch or it
            // falls through to the generic Exception bucket below as a misleading UNKNOWN.
            Log.w(TAG, "Couldn't convert Nominatim's response", e)
            Result.Error(DataError.Network.SERIALIZATION)
        } catch (e: UnknownHostException) {
            // The one case that genuinely means "no network/DNS" — distinct from any other IOException
            // (connection reset, SSL handshake, etc.), which isn't actually a lack-of-internet problem.
            Log.w(TAG, "Couldn't resolve the search host", e)
            Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: IOException) {
            // A generic connection failure (reset, SSL handshake, etc.) — not necessarily "no
            // internet" (that's UnknownHostException above) and not a server-returned error either,
            // so DataError.Network.UNKNOWN is the honest bucket, not SERVER_ERROR or NO_INTERNET.
            Log.w(TAG, "Search request failed", e)
            Result.Error(DataError.Network.UNKNOWN)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected search failure", e)
            Result.Error(DataError.Network.UNKNOWN)
        }
}

private fun HttpStatusCode.toNetworkError(): DataError.Network =
    when (value) {
        408 -> DataError.Network.REQUEST_TIMEOUT
        429 -> DataError.Network.TOO_MANY_REQUESTS
        in 500..599 -> DataError.Network.SERVER_ERROR
        else -> DataError.Network.UNKNOWN
    }
