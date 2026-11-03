package com.routeforge.addresssearch.data

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.AddressLabel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.UnknownHostException
import java.util.Locale
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

// The app's own client ignores the fields it does not read (`HttpClientFactory`); the real answers carry many of them.
private val appJson = Json { ignoreUnknownKeys = true }

private fun reverseClient(
    engine: MockEngine,
    spacing: RequestSpacing = RequestSpacing(TimeSource.Monotonic),
): NominatimReverseGeocoder = NominatimReverseGeocoder(HttpClient(engine) { install(ContentNegotiation) { json(appJson) } }, spacing)

private fun answering(body: String) =
    MockEngine {
        respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

class NominatimReverseGeocoderTest {
    @Test
    fun `a place with a name is described by it, with the display name as the full text`() =
        runTest {
            val body =
                """{"display_name": "Brandenburger Tor, Pariser Platz, Mitte, Berlin", "name": "Brandenburger Tor",
                    "address": {"road": "Pariser Platz", "house_number": "1"}}"""
            val client = reverseClient(answering(body))

            val result = client.addressAt(52.5163, 13.3777)

            val expected = AddressLabel(full = "Brandenburger Tor, Pariser Platz, Mitte, Berlin", short = "Brandenburger Tor")
            assertEquals(Result.Success(expected), result)
        }

    @Test
    fun `without a name the short text is the street and the house number`() =
        runTest {
            val body = """{"display_name": "1, Pariser Platz, Mitte, Berlin", "address": {"road": "Pariser Platz", "house_number": "1"}}"""

            val result = reverseClient(answering(body)).addressAt(52.5163, 13.3777)

            assertEquals(Result.Success(AddressLabel(full = "1, Pariser Platz, Mitte, Berlin", short = "Pariser Platz 1")), result)
        }

    @Test
    fun `without a name or a number the short text is the street`() =
        runTest {
            val body = """{"display_name": "Pariser Platz, Mitte, Berlin", "address": {"road": "Pariser Platz"}}"""

            val result = reverseClient(answering(body)).addressAt(52.5163, 13.3777)

            assertEquals(Result.Success(AddressLabel(full = "Pariser Platz, Mitte, Berlin", short = "Pariser Platz")), result)
        }

    @Test
    fun `without a name or a street the short text is the first part of the display name`() =
        runTest {
            val body = """{"display_name": "Mitte, Berlin, Germany", "address": {}}"""

            val result = reverseClient(answering(body)).addressAt(52.5163, 13.3777)

            assertEquals(Result.Success(AddressLabel(full = "Mitte, Berlin, Germany", short = "Mitte")), result)
        }

    @Test
    fun `blank pieces are skipped as if they were missing`() =
        runTest {
            val body = """{"display_name": "Somewhere, Else", "name": "  ", "address": {"road": " ", "house_number": " "}}"""

            val result = reverseClient(answering(body)).addressAt(1.0, 2.0)

            assertEquals(Result.Success(AddressLabel(full = "Somewhere, Else", short = "Somewhere")), result)
        }

    @Test
    fun `a real answer of the service, with all its extra fields, is read`() =
        runTest {
            // Taken from the service on 2026-11-03 for 52.5163, 13.3777; the fields the app does not use must be ignored.
            val body =
                """
                {"place_id":144588537,"licence":"Data (c) OpenStreetMap contributors, ODbL 1.0. http://osm.org/copyright",
                 "osm_type":"node","osm_id":3786417057,"lat":"52.5162700","lon":"13.3777021","category":"tourism","type":"artwork",
                 "place_rank":30,"importance":8.875486381318407e-05,"addresstype":"tourism","name":"Quadriga with Victoria",
                 "display_name":"Quadriga with Victoria, 1, Pariser Platz, Friedrich-Wilhelm-Stadt, Mitte, Berlin, 10117, Germany",
                 "address":{"tourism":"Quadriga with Victoria","house_number":"1","road":"Pariser Platz",
                 "quarter":"Friedrich-Wilhelm-Stadt","suburb":"Mitte","borough":"Mitte","city":"Berlin","ISO3166-2-lvl4":"DE-BE",
                 "postcode":"10117","country":"Germany","country_code":"de"},
                 "boundingbox":["52.5162200","52.5163200","13.3776521","13.3777521"]}
                """.trimIndent()

            val result = reverseClient(answering(body)).addressAt(52.5163, 13.3777)

            val full = "Quadriga with Victoria, 1, Pariser Platz, Friedrich-Wilhelm-Stadt, Mitte, Berlin, 10117, Germany"
            assertEquals(Result.Success(AddressLabel(full = full, short = "Quadriga with Victoria")), result)
        }

    @Test
    fun `an answer with an error field means nothing was found`() =
        runTest {
            val result = reverseClient(answering("""{"error": "Unable to geocode"}""")).addressAt(0.0, -30.0)

            assertEquals(Result.Success(null), result)
        }

    @Test
    fun `an answer with no usable text means nothing was found`() =
        runTest {
            val result = reverseClient(answering("""{"display_name": "  ", "address": {}}""")).addressAt(0.0, -30.0)

            assertEquals(Result.Success(null), result)
        }

    @Test
    fun `the request carries the position, the options and the device language`() =
        runTest {
            val saved = Locale.getDefault()
            Locale.setDefault(Locale.GERMANY)
            try {
                var captured: HttpRequestData? = null
                val engine =
                    MockEngine { request ->
                        captured = request
                        respond(
                            content = """{"error": "Unable to geocode"}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    }

                reverseClient(engine).addressAt(52.5163, 13.3777)

                val request = requireNotNull(captured)
                assertEquals("/reverse", request.url.encodedPath)
                assertEquals("52.516300", request.url.parameters["lat"])
                assertEquals("13.377700", request.url.parameters["lon"])
                assertEquals("jsonv2", request.url.parameters["format"])
                assertEquals("18", request.url.parameters["zoom"])
                assertEquals("1", request.url.parameters["addressdetails"])
                assertEquals("de-DE", request.headers[HttpHeaders.AcceptLanguage])
            } finally {
                Locale.setDefault(saved)
            }
        }

    @Test
    fun `a 500 response maps to SERVER_ERROR`() =
        runTest {
            val result = reverseClient(MockEngine { respondError(HttpStatusCode.InternalServerError) }).addressAt(1.0, 2.0)

            assertEquals(Result.Error(DataError.Network.SERVER_ERROR), result)
        }

    @Test
    fun `a 429 response maps to TOO_MANY_REQUESTS`() =
        runTest {
            val result = reverseClient(MockEngine { respondError(HttpStatusCode.TooManyRequests) }).addressAt(1.0, 2.0)

            assertEquals(Result.Error(DataError.Network.TOO_MANY_REQUESTS), result)
        }

    @Test
    fun `a 403 response maps to FORBIDDEN`() =
        runTest {
            val result = reverseClient(MockEngine { respondError(HttpStatusCode.Forbidden) }).addressAt(1.0, 2.0)

            assertEquals(Result.Error(DataError.Network.FORBIDDEN), result)
        }

    @Test
    fun `a body that is not readable maps to SERIALIZATION`() =
        runTest {
            val result = reverseClient(answering("this is not json")).addressAt(1.0, 2.0)

            assertEquals(Result.Error(DataError.Network.SERIALIZATION), result)
        }

    @Test
    fun `a timeout maps to REQUEST_TIMEOUT`() =
        runTest {
            val result = reverseClient(MockEngine { throw HttpRequestTimeoutException("https://example.org", 1L) }).addressAt(1.0, 2.0)

            assertEquals(Result.Error(DataError.Network.REQUEST_TIMEOUT), result)
        }

    @Test
    fun `an unknown host maps to NO_INTERNET`() =
        runTest {
            val result = reverseClient(MockEngine { throw UnknownHostException("nominatim.openstreetmap.org") }).addressAt(1.0, 2.0)

            assertEquals(Result.Error(DataError.Network.NO_INTERNET), result)
        }

    @Test
    fun `any other connection failure maps to UNKNOWN`() =
        runTest {
            val result = reverseClient(MockEngine { throw IOException("connection reset") }).addressAt(1.0, 2.0)

            assertEquals(Result.Error(DataError.Network.UNKNOWN), result)
        }

    @Test
    fun `two lookups of different spots reach the service at least a second apart`() =
        runTest {
            val reachedAt = mutableListOf<Long>()
            val engine =
                MockEngine {
                    reachedAt.add(currentTime)
                    respond(
                        content = """{"error": "Unable to geocode"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val client = reverseClient(engine, RequestSpacing(testTimeSource))

            client.addressAt(1.0, 1.0)
            client.addressAt(2.0, 2.0)

            assertEquals(listOf(0L, 1_000L), reachedAt)
        }

    @Test
    fun `two lookups of the same spot reach the service once`() =
        runTest {
            var reached = 0
            val engine =
                MockEngine {
                    reached++
                    respond(
                        content = """{"display_name": "Somewhere, Else"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val client = reverseClient(engine, RequestSpacing(testTimeSource))

            client.addressAt(1.0, 1.0)
            client.addressAt(1.0, 1.0)

            assertEquals(1, reached)
        }

    @Test
    fun `a refusal with 403 or 429 stops all requests for ten minutes`() =
        runTest {
            var reached = 0
            val engine =
                MockEngine {
                    reached++
                    respondError(HttpStatusCode.TooManyRequests)
                }
            val client = reverseClient(engine, RequestSpacing(testTimeSource))
            client.addressAt(1.0, 1.0)

            delay(9.minutes)
            val duringThePause = client.addressAt(2.0, 2.0)

            assertEquals(1, reached)
            assertEquals(Result.Error(DataError.Network.TOO_MANY_REQUESTS), duringThePause)
        }

    @Test
    fun `a 500 answer does not start the pause`() =
        runTest {
            var reached = 0
            val engine =
                MockEngine {
                    reached++
                    respondError(HttpStatusCode.InternalServerError)
                }
            val client = reverseClient(engine, RequestSpacing(testTimeSource))

            client.addressAt(1.0, 1.0)
            client.addressAt(2.0, 2.0)

            assertEquals(2, reached)
        }
}
