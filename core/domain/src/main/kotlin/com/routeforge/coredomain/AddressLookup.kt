package com.routeforge.coredomain

import com.routeforge.coredomain.model.AddressLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Looks up the address of the one position a confirmation is open for. It waits a moment before asking, so a confirmation closed at
 * once costs no request, and each [start] replaces the previous one of this instance, so an answer for a spot the user has moved on
 * from is never delivered. A failed lookup and "nothing found" deliver nothing.
 */
class AddressLookup(
    private val reverseGeocoder: ReverseGeocoder,
) {
    private var job: Job? = null

    fun start(
        scope: CoroutineScope,
        latitude: Double,
        longitude: Double,
        onFound: (AddressLabel) -> Unit,
    ) {
        job?.cancel()
        job =
            scope.launch {
                delay(DEBOUNCE_MILLIS)
                val answer = reverseGeocoder.addressAt(latitude, longitude)
                if (answer is Result.Success) answer.data?.let(onFound)
            }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    companion object {
        const val DEBOUNCE_MILLIS = 400L
    }
}
