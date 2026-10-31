package com.routeforge.coredomain

import com.routeforge.coredomain.model.AddressLabel

interface ReverseGeocoder {
    /** `Success(null)` means nothing was found at the position; an `Error` is any failure to ask. */
    suspend fun addressAt(
        latitude: Double,
        longitude: Double,
    ): Result<AddressLabel?, DataError.Network>
}
