package com.routeforge.addresssearch.data

import com.routeforge.coredomain.model.PlaceSearchResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** Matches Nominatim's `format=jsonv2` response shape (only the fields this app actually uses). */
@Serializable
data class NominatimResultDto(
    @SerialName("display_name") val displayName: String,
    val name: String? = null,
    val lat: String,
    val lon: String,
    val importance: Double = 0.0,
)

/** Returns `null` if [NominatimResultDto.lat]/[NominatimResultDto.lon] aren't parseable numbers —
 *  callers skip such entries rather than crash on a malformed response. */
fun NominatimResultDto.toPlaceSearchResult(): PlaceSearchResult? {
    val latitude = lat.toDoubleOrNull() ?: return null
    val longitude = lon.toDoubleOrNull() ?: return null
    return PlaceSearchResult(
        name = name?.takeIf { it.isNotBlank() } ?: displayName.substringBefore(","),
        formattedAddress = displayName,
        latitude = latitude,
        longitude = longitude,
        importance = importance,
    )
}
