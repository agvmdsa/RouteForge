package com.routeforge.coredata

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.UnknownHostException

class HttpRedirectSourceTest {
    private val start = "https://maps.app.goo.gl/AbCdEf123"
    private val target = "https://www.google.com/maps/place/Berlin/@52.52,13.405,12z"

    private fun sourceAnswering(
        requests: MutableList<HttpRequestData> = mutableListOf(),
        answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpRedirectSource {
        val engine =
            MockEngine { request ->
                requests += request
                answer(request)
            }
        // The client handed in follows redirects like Ktor's default one; the source must switch that off itself.
        return HttpRedirectSource(HttpClient(engine))
    }

    private fun MockRequestHandleScope.redirecting(
        status: HttpStatusCode,
        location: String?,
    ) = respond(
        content = "",
        status = status,
        headers = if (location == null) headersOf() else headersOf(HttpHeaders.Location, location),
    )

    @Test
    fun `each kind of redirect gives the address it points to`() =
        runBlocking {
            for (status in listOf(301, 302, 303, 307, 308)) {
                val source = sourceAnswering { redirecting(HttpStatusCode.fromValue(status), target) }

                assertEquals(Result.Success(target), source.locationOf(start), "status $status")
            }
        }

    @Test
    fun `a relative location is resolved against the address that was asked`() =
        runBlocking {
            val source = sourceAnswering { redirecting(HttpStatusCode.Found, "/maps/place/Berlin") }

            assertEquals(Result.Success("https://maps.app.goo.gl/maps/place/Berlin"), source.locationOf(start))
        }

    @Test
    fun `an answer that is not a redirect, or has no location, leads nowhere`() =
        runBlocking {
            assertEquals(Result.Success(null), sourceAnswering { redirecting(HttpStatusCode.OK, null) }.locationOf(start))
            assertEquals(Result.Success(null), sourceAnswering { redirecting(HttpStatusCode.OK, target) }.locationOf(start))
            assertEquals(Result.Success(null), sourceAnswering { redirecting(HttpStatusCode.Found, null) }.locationOf(start))
            assertEquals(Result.Success(null), sourceAnswering { redirecting(HttpStatusCode.Found, "") }.locationOf(start))
        }

    @Test
    fun `an error status is an error`() =
        runBlocking {
            assertEquals(
                Result.Error(DataError.Network.NOT_FOUND),
                sourceAnswering { redirecting(HttpStatusCode.NotFound, null) }.locationOf(start),
            )
            assertEquals(
                Result.Error(DataError.Network.SERVER_ERROR),
                sourceAnswering { redirecting(HttpStatusCode.InternalServerError, null) }.locationOf(start),
            )
        }

    @Test
    fun `a failure of the connection is an error`() =
        runBlocking {
            assertEquals(
                Result.Error(DataError.Network.NO_INTERNET),
                sourceAnswering { throw UnknownHostException("maps.app.goo.gl") }.locationOf(start),
            )
            assertEquals(
                Result.Error(DataError.Network.UNKNOWN),
                sourceAnswering { throw IOException("reset") }.locationOf(start),
            )
        }

    @Test
    fun `a timeout is an error`() =
        runBlocking {
            val source = sourceAnswering { throw HttpRequestTimeoutException(start, 5_000) }

            assertEquals(Result.Error(DataError.Network.REQUEST_TIMEOUT), source.locationOf(start))
        }

    @Test
    fun `it asks once with GET and does not follow the redirect itself`() =
        runBlocking {
            val requests = mutableListOf<HttpRequestData>()
            val source = sourceAnswering(requests) { redirecting(HttpStatusCode.Found, target) }

            source.locationOf(start)

            assertEquals(1, requests.size)
            assertEquals(HttpMethod.Get, requests.single().method)
            assertEquals(start, requests.single().url.toString())
        }
}
