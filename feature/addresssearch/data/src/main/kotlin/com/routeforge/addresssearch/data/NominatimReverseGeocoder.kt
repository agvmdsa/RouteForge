package com.routeforge.addresssearch.data

import android.util.Log
import com.routeforge.coredata.toNetworkError
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.ReverseGeocoder
import com.routeforge.coredomain.model.AddressLabel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.UnknownHostException
import java.util.Locale

private const val TAG = "NominatimReverseGeocoder"
private const val REVERSE_URL = "https://nominatim.openstreetmap.org/reverse"
private const val STREET_LEVEL_ZOOM = 18
private const val COORDINATE_FORMAT = "%.6f"

internal class NominatimReverseGeocoder(
    private val httpClient: HttpClient,
    private val spacing: RequestSpacing,
) : ReverseGeocoder {
    override suspend fun addressAt(
        latitude: Double,
        longitude: Double,
    ): Result<AddressLabel?, DataError.Network> = spacing.addressFor(latitude, longitude) { request(latitude, longitude) }

    private suspend fun request(
        latitude: Double,
        longitude: Double,
    ): Result<AddressLabel?, DataError.Network> =
        try {
            val response =
                httpClient.get(REVERSE_URL) {
                    // Formatted, not `toString()`, so a tiny value never reaches the service as "1.0E-5".
                    parameter("lat", COORDINATE_FORMAT.format(Locale.US, latitude))
                    parameter("lon", COORDINATE_FORMAT.format(Locale.US, longitude))
                    parameter("format", "jsonv2")
                    parameter("zoom", STREET_LEVEL_ZOOM)
                    parameter("addressdetails", 1)
                    header(HttpHeaders.AcceptLanguage, Locale.getDefault().toLanguageTag())
                }
            if (!response.status.isSuccess()) {
                Log.w(TAG, "Nominatim returned ${response.status}")
                Result.Error(response.status.toNetworkError())
            } else {
                Result.Success(response.body<NominatimReverseDto>().toAddressLabel())
            }
        } catch (e: HttpRequestTimeoutException) {
            // Before CancellationException: Ktor's timeout plugin throws a subclass of it.
            Log.w(TAG, "Reverse lookup timed out", e)
            Result.Error(DataError.Network.REQUEST_TIMEOUT)
        } catch (e: SerializationException) {
            Log.w(TAG, "Couldn't parse Nominatim's answer", e)
            Result.Error(DataError.Network.SERIALIZATION)
        } catch (e: JsonConvertException) {
            Log.w(TAG, "Couldn't convert Nominatim's answer", e)
            Result.Error(DataError.Network.SERIALIZATION)
        } catch (e: UnknownHostException) {
            Log.w(TAG, "Couldn't resolve the host", e)
            Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: IOException) {
            Log.w(TAG, "Reverse lookup failed", e)
            Result.Error(DataError.Network.UNKNOWN)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected reverse lookup failure", e)
            Result.Error(DataError.Network.UNKNOWN)
        }
}
