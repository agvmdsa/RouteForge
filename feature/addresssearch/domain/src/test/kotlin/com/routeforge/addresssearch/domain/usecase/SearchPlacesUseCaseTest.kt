package com.routeforge.addresssearch.domain.usecase

import com.routeforge.addresssearch.domain.testing.FakeGeocodeClient
import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.LastKnownRealLocationHolder
import com.routeforge.coredomain.model.RealLocation
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SearchPlacesUseCaseTest {
    private val geocodeClient = FakeGeocodeClient()
    private val lastKnownRealLocationHolder = LastKnownRealLocationHolder()
    private val useCase = SearchPlacesUseCase(geocodeClient, RankSearchResultsUseCase(), lastKnownRealLocationHolder)

    @Test
    fun `prefers the real known location over the fallback reference point`() =
        runTest {
            lastKnownRealLocationHolder.set(RealLocation(latitude = 1.0, longitude = 1.0))
            val near = place("Near real location", latitude = 1.0, longitude = 1.0, importance = 0.1)
            val far = place("Near fallback only", latitude = 50.0, longitude = 50.0, importance = 0.1)
            geocodeClient.result = Result.Success(listOf(far, near))

            val result = useCase("query", fallbackReferenceLatitude = 50.0, fallbackReferenceLongitude = 50.0)

            assertEquals(Result.Success(listOf(near, far)), result)
        }

    @Test
    fun `falls back to the caller-provided reference point when there's no real location`() =
        runTest {
            val near = place("Near fallback", latitude = 50.0, longitude = 50.0, importance = 0.1)
            val far = place("Far from fallback", latitude = 1.0, longitude = 1.0, importance = 0.1)
            geocodeClient.result = Result.Success(listOf(far, near))

            val result = useCase("query", fallbackReferenceLatitude = 50.0, fallbackReferenceLongitude = 50.0)

            assertEquals(Result.Success(listOf(near, far)), result)
        }

    @Test
    fun `ranks by importance alone when neither a real location nor a fallback point is known`() =
        runTest {
            val lessImportant = place("Less important", latitude = 0.0, longitude = 0.0, importance = 0.1)
            val moreImportant = place("More important", latitude = 90.0, longitude = 90.0, importance = 0.9)
            geocodeClient.result = Result.Success(listOf(lessImportant, moreImportant))

            val result = useCase("query", fallbackReferenceLatitude = null, fallbackReferenceLongitude = null)

            assertEquals(Result.Success(listOf(moreImportant, lessImportant)), result)
        }

    @Test
    fun `passes a geocode failure straight through`() =
        runTest {
            geocodeClient.result = Result.Error(DataError.Network.NO_INTERNET)

            val result = useCase("query", fallbackReferenceLatitude = null, fallbackReferenceLongitude = null)

            assertEquals(Result.Error(DataError.Network.NO_INTERNET), result)
        }

    @Test
    fun `forwards the query unchanged to the geocode client`() =
        runTest {
            useCase("Rua Example, 123", fallbackReferenceLatitude = null, fallbackReferenceLongitude = null)

            assertEquals("Rua Example, 123", geocodeClient.lastQuery)
        }
}
