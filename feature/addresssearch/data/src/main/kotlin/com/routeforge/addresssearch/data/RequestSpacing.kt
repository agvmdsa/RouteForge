package com.routeforge.addresssearch.data

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.AddressLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val MIN_INTERVAL = 1.seconds
private val REFUSAL_PAUSE = 10.minutes
private const val POSITIONS_PER_DEGREE = 100_000.0

/**
 * How the address service is asked, to keep within its usage rules: at least [MIN_INTERVAL] between the starts of two requests
 * (a request that comes sooner waits), the latest answer reused for the same spot (about a metre) so it is not asked twice in a row,
 * and no request at all for [REFUSAL_PAUSE] after the service refuses the app. Requests are served one at a time.
 */
internal class RequestSpacing(
    private val timeSource: TimeSource,
) {
    private val oneAtATime = Mutex()
    private var lastStart: TimeMark? = null
    private var pausedUntil: TimeMark? = null
    private var latest: RememberedAnswer? = null

    suspend fun addressFor(
        latitude: Double,
        longitude: Double,
        ask: suspend () -> Result<AddressLabel?, DataError.Network>,
    ): Result<AddressLabel?, DataError.Network> =
        oneAtATime.withLock {
            val spot = Spot(Math.round(latitude * POSITIONS_PER_DEGREE), Math.round(longitude * POSITIONS_PER_DEGREE))
            latest?.takeIf { it.spot == spot }?.let { return@withLock it.result }
            if (pausedUntil?.hasNotPassedNow() == true) return@withLock Result.Error(DataError.Network.TOO_MANY_REQUESTS)
            waitForTurn()
            lastStart = timeSource.markNow()
            val result = ask()
            when (result) {
                is Result.Success -> latest = RememberedAnswer(spot, result)
                is Result.Error -> if (result.error.isRefusal()) pausedUntil = timeSource.markNow() + REFUSAL_PAUSE
            }
            result
        }

    private suspend fun waitForTurn() {
        val sinceLastStart = lastStart?.elapsedNow() ?: return
        if (sinceLastStart < MIN_INTERVAL) delay(MIN_INTERVAL - sinceLastStart)
    }

    private fun DataError.Network.isRefusal() = this == DataError.Network.FORBIDDEN || this == DataError.Network.TOO_MANY_REQUESTS

    private data class Spot(
        val latitudeInHundredThousandths: Long,
        val longitudeInHundredThousandths: Long,
    )

    private class RememberedAnswer(
        val spot: Spot,
        val result: Result<AddressLabel?, DataError.Network>,
    )
}
