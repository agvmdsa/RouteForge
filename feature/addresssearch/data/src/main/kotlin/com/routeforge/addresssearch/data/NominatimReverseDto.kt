package com.routeforge.addresssearch.data

import com.routeforge.coredomain.model.AddressLabel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Matches Nominatim's `reverse` answer in `format=jsonv2` (only the fields this app uses); every field is optional. */
@Serializable
internal data class NominatimReverseDto(
    @SerialName("display_name") val displayName: String? = null,
    val name: String? = null,
    val address: NominatimAddressDto? = null,
    val error: String? = null,
)

@Serializable
internal data class NominatimAddressDto(
    val road: String? = null,
    @SerialName("house_number") val houseNumber: String? = null,
)

/**
 * `null` when the service found nothing (an `error` field) or gave no usable text. The short text is the place name, else the
 * street with its number, else the first part of the display name.
 */
internal fun NominatimReverseDto.toAddressLabel(): AddressLabel? {
    if (error != null) return null
    val full = displayName.cleaned() ?: name.cleaned() ?: return null
    val road = address?.road.cleaned()
    val streetAndNumber = road?.let { listOfNotNull(it, address?.houseNumber.cleaned()).joinToString(" ") }
    val short = name.cleaned() ?: streetAndNumber ?: full.substringBefore(",").cleaned() ?: full
    return AddressLabel(full = full, short = short)
}

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
