package com.routeforge.coredomain.testing

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.ReverseGeocoder
import com.routeforge.coredomain.model.AddressLabel
import kotlinx.coroutines.CompletableDeferred

/**
 * Answers with [answer]. After [hold] it keeps every request waiting until [release], so a test can move the user on while a
 * lookup is still in flight. It records every request in [requests] and applies no rule of its own.
 */
class FakeReverseGeocoder(
    var answer: Result<AddressLabel?, DataError.Network> = Result.Success(null),
) : ReverseGeocoder {
    val requests = mutableListOf<Pair<Double, Double>>()

    private var gate: CompletableDeferred<Unit>? = null

    fun hold() {
        gate = CompletableDeferred()
    }

    fun release() {
        gate?.complete(Unit)
        gate = null
    }

    override suspend fun addressAt(
        latitude: Double,
        longitude: Double,
    ): Result<AddressLabel?, DataError.Network> {
        requests.add(latitude to longitude)
        gate?.await()
        return answer
    }
}
