package com.routeforge.addresssearch.data

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun mockClient(engine: MockEngine): HttpClient = HttpClient(engine) { install(ContentNegotiation) { json() } }

class NominatimGeocodeClientTest {
    @Test
    fun `a successful search maps every result and skips entries with unparseable coordinates`() =
        runTest {
            val body =
                """
                [
                  {"display_name": "Eiffel Tower, Paris, France", "name": "Eiffel Tower", "lat": "48.8584", "lon": "2.2945", "importance": 0.8},
                  {"display_name": "Nowhere", "lat": "oops", "lon": "2.0", "importance": 0.1}
                ]
                """.trimIndent()
            val engine =
                MockEngine { respond(content = body, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) }
            val client = NominatimGeocodeClient(mockClient(engine))

            val result = client.search("Eiffel Tower")

            assertTrue(result is Result.Success)
            val places = (result as Result.Success).data
            assertEquals(1, places.size)
            assertEquals("Eiffel Tower", places.first().name)
            assertEquals(48.8584, places.first().latitude)
        }

    @Test
    fun `an empty results array maps to an empty, successful list`() =
        runTest {
            val engine =
                MockEngine { respond(content = "[]", status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) }
            val client = NominatimGeocodeClient(mockClient(engine))

            val result = client.search("asdkjhaskjdh")

            assertEquals(Result.Success(emptyList<Any>()), result)
        }

    @Test
    fun `a 429 rate-limit response maps to TOO_MANY_REQUESTS`() =
        runTest {
            val engine = MockEngine { respondError(HttpStatusCode.TooManyRequests) }
            val client = NominatimGeocodeClient(mockClient(engine))

            val result = client.search("anything")

            assertEquals(Result.Error(DataError.Network.TOO_MANY_REQUESTS), result)
        }

    @Test
    fun `a 500 server error response maps to SERVER_ERROR`() =
        runTest {
            val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
            val client = NominatimGeocodeClient(mockClient(engine))

            val result = client.search("anything")

            assertEquals(Result.Error(DataError.Network.SERVER_ERROR), result)
        }
}
