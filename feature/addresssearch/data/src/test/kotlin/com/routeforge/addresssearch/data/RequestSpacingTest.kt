package com.routeforge.addresssearch.data

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.AddressLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RequestSpacingTest {
    private val label = AddressLabel(full = "Pariser Platz 1, Mitte, Berlin", short = "Pariser Platz 1")

    @Test
    fun `the first request starts at once`() =
        runTest {
            val spacing = RequestSpacing(testTimeSource)
            var startedAt = -1L

            spacing.addressFor(52.5, 13.4) {
                startedAt = currentTime
                Result.Success(label)
            }

            assertEquals(0L, startedAt)
        }

    @Test
    fun `a second request for another spot starts at least a second after the first started`() =
        runTest {
            val spacing = RequestSpacing(testTimeSource)
            val startedAt = mutableListOf<Long>()
            val ask: suspend () -> Result<AddressLabel?, DataError.Network> = {
                startedAt.add(currentTime)
                Result.Success(label)
            }

            spacing.addressFor(1.0, 1.0, ask)
            spacing.addressFor(2.0, 2.0, ask)

            assertEquals(listOf(0L, 1_000L), startedAt)
        }

    @Test
    fun `the same spot twice asks once and returns the same answer`() =
        runTest {
            val spacing = RequestSpacing(testTimeSource)
            var asked = 0
            val ask: suspend () -> Result<AddressLabel?, DataError.Network> = {
                asked++
                Result.Success(label)
            }

            spacing.addressFor(52.5163, 13.3777, ask)
            val again = spacing.addressFor(52.5163, 13.3777, ask)

            assertEquals(1, asked)
            assertEquals(Result.Success(label), again)
            assertEquals(0L, currentTime)
        }

    @Test
    fun `a spot within a metre of the remembered one counts as the same spot`() =
        runTest {
            val spacing = RequestSpacing(testTimeSource)
            var asked = 0
            val ask: suspend () -> Result<AddressLabel?, DataError.Network> = {
                asked++
                Result.Success(label)
            }

            spacing.addressFor(52.516300, 13.377700, ask)
            spacing.addressFor(52.516304, 13.377704, ask)

            assertEquals(1, asked)
        }

    @Test
    fun `a different spot replaces the remembered answer`() =
        runTest {
            val spacing = RequestSpacing(testTimeSource)
            var asked = 0
            val ask: suspend () -> Result<AddressLabel?, DataError.Network> = {
                asked++
                Result.Success(label)
            }

            spacing.addressFor(1.0, 1.0, ask)
            spacing.addressFor(2.0, 2.0, ask)
            spacing.addressFor(1.0, 1.0, ask)

            assertEquals(3, asked)
        }

    @Test
    fun `an answer of nothing found is remembered too`() =
        runTest {
            val spacing = RequestSpacing(testTimeSource)
            var asked = 0
            val ask: suspend () -> Result<AddressLabel?, DataError.Network> = {
                asked++
                Result.Success(null)
            }

            spacing.addressFor(0.0, -30.0, ask)
            val again = spacing.addressFor(0.0, -30.0, ask)

            assertEquals(1, asked)
            assertEquals(Result.Success(null), again)
        }

    @Test
    fun `a failed request remembers nothing`() =
        runTest {
            val spacing = RequestSpacing(testTimeSource)
            var asked = 0
            val ask: suspend () -> Result<AddressLabel?, DataError.Network> = {
                asked++
                Result.Error(DataError.Network.NO_INTERNET)
            }

            spacing.addressFor(1.0, 1.0, ask)
            spacing.addressFor(1.0, 1.0, ask)

            assertEquals(2, asked)
        }

    @Test
    fun `after the service refuses the app no request is made for ten minutes`() =
        runTest {
            val spacing = RequestSpacing(testTimeSource)
            var asked = 0
            spacing.addressFor(1.0, 1.0) {
                asked++
                Result.Error(DataError.Network.TOO_MANY_REQUESTS)
            }

            delay(10.minutes - 1.seconds)
            val duringThePause =
                spacing.addressFor(2.0, 2.0) {
                    asked++
                    Result.Success(label)
                }

            assertEquals(1, asked)
            assertEquals(Result.Error(DataError.Network.TOO_MANY_REQUESTS), duringThePause)
        }

    @Test
    fun `after the pause the next request goes out normally`() =
        runTest {
            val spacing = RequestSpacing(testTimeSource)
            spacing.addressFor(1.0, 1.0) { Result.Error(DataError.Network.FORBIDDEN) }

            delay(10.minutes + 1.seconds)
            val afterThePause = spacing.addressFor(2.0, 2.0) { Result.Success(label) }

            assertEquals(Result.Success(label), afterThePause)
        }

    @Test
    fun `an ordinary server error does not start the pause`() =
        runTest {
            val spacing = RequestSpacing(testTimeSource)
            var asked = 0
            val ask: suspend () -> Result<AddressLabel?, DataError.Network> = {
                asked++
                Result.Error(DataError.Network.SERVER_ERROR)
            }

            spacing.addressFor(1.0, 1.0, ask)
            spacing.addressFor(2.0, 2.0, ask)

            assertEquals(2, asked)
        }
}
