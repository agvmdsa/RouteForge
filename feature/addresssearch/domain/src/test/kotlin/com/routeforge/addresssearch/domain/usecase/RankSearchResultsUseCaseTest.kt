package com.routeforge.addresssearch.domain.usecase

import com.routeforge.coredomain.model.PlaceSearchResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private fun place(
    name: String,
    latitude: Double,
    longitude: Double,
    importance: Double,
) = PlaceSearchResult(name = name, formattedAddress = name, latitude = latitude, longitude = longitude, importance = importance)

class RankSearchResultsUseCaseTest {
    private val rankSearchResults = RankSearchResultsUseCase()

    @Test
    fun `with equal importance, the closer result ranks first`() {
        val far = place("Far", latitude = 1.0, longitude = 1.0, importance = 0.9)
        val close = place("Close", latitude = 0.001, longitude = 0.001, importance = 0.9)

        val ranked = rankSearchResults(listOf(far, close), referenceLatitude = 0.0, referenceLongitude = 0.0)

        assertEquals(listOf(close, far), ranked)
    }

    @Test
    fun `a much closer but less important result can still outrank a far but more important one`() {
        val closeButObscure = place("Obscure", latitude = 0.001, longitude = 0.001, importance = 0.1)
        val farButFamous = place("Famous", latitude = 1.0, longitude = 1.0, importance = 0.9)

        val ranked =
            rankSearchResults(listOf(farButFamous, closeButObscure), referenceLatitude = 0.0, referenceLongitude = 0.0)

        assertEquals(listOf(closeButObscure, farButFamous), ranked)
    }

    @Test
    fun `falls back to importance-only ordering when no reference point is available`() {
        val lessImportant = place("Lesser", latitude = 50.0, longitude = 50.0, importance = 0.3)
        val moreImportant = place("Greater", latitude = -50.0, longitude = -50.0, importance = 0.9)

        val ranked =
            rankSearchResults(listOf(lessImportant, moreImportant), referenceLatitude = null, referenceLongitude = null)

        assertEquals(listOf(moreImportant, lessImportant), ranked)
    }
}
