package com.routeforge.coredata

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.link.RedirectSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.URI
import java.net.UnknownHostException

private val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)
private val ABSOLUTE_WEB_ADDRESS = Regex("""^https?://""", RegexOption.IGNORE_CASE)

/**
 * Learns where an address redirects to from the answer's status and `Location` header alone: one GET, the redirect is not
 * followed (that is the caller's decision, within its limits) and the body is never read. What leaves the device is the
 * address asked for and the app's identification, nothing about the device or its position.
 */
internal class HttpRedirectSource(
    client: HttpClient,
) : RedirectSource {
    private val client = client.config { followRedirects = false }

    override suspend fun locationOf(url: String): Result<String?, DataError.Network> =
        try {
            client.prepareGet(url).execute { response -> answerOf(url, response) }
        } catch (e: HttpRequestTimeoutException) {
            // Before CancellationException: Ktor's timeout must surface as an error, not as a silent cancellation.
            Result.Error(DataError.Network.REQUEST_TIMEOUT)
        } catch (e: UnknownHostException) {
            Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: IOException) {
            Result.Error(DataError.Network.UNKNOWN)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Network.UNKNOWN)
        }

    private fun answerOf(
        requested: String,
        response: HttpResponse,
    ): Result<String?, DataError.Network> {
        val status = response.status
        return when {
            status.value in REDIRECT_STATUSES -> Result.Success(locationOf(requested, response.headers[HttpHeaders.Location]))
            status.value >= 400 -> Result.Error(status.toNetworkError())
            else -> Result.Success(null)
        }
    }

    /** An absolute location is used as it came; a relative one is resolved against the address that was asked. */
    private fun locationOf(
        requested: String,
        location: String?,
    ): String? {
        val trimmed = location?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> null
            ABSOLUTE_WEB_ADDRESS.containsMatchIn(trimmed) -> trimmed
            else -> runCatching { URI(requested).resolve(trimmed).toString() }.getOrNull()
        }
    }
}
