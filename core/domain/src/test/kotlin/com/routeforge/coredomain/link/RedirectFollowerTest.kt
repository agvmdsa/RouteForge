package com.routeforge.coredomain.link

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.testing.FakeRedirectSource
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RedirectFollowerTest {
    private class NeverAnswers : RedirectSource {
        override suspend fun locationOf(url: String): Result<String?, DataError.Network> = awaitCancellation()
    }

    private fun chain(vararg addresses: String): Map<String, Result<String?, DataError.Network>> =
        addresses.toList().zipWithNext().associate { (from, to) -> from to Result.Success(to) }

    private val startOfChain = "https://maps.app.goo.gl/a0"
    private val isMapsPlace = { address: String -> address.contains("/maps/place/") }

    @Test
    fun `follows a short link to the first address that is useful`() =
        runBlocking {
            val place = "https://www.google.com/maps/place/Berlin/@52.52,13.405,12z"
            val source = FakeRedirectSource(chain(startOfChain, "https://consent.google.com/x", place))

            val found = RedirectFollower(source).follow(startOfChain, isMapsPlace)

            assertEquals(place, found)
            assertEquals(listOf(startOfChain, "https://consent.google.com/x"), source.requested)
        }

    @Test
    fun `the start address is tested before any request is made`() =
        runBlocking {
            val place = "https://www.google.com/maps/place/Berlin/@52.52,13.405,12z"
            val source = FakeRedirectSource(emptyMap())

            assertEquals(place, RedirectFollower(source).follow(place, isMapsPlace))
            assertEquals(emptyList<String>(), source.requested)
        }

    @Test
    fun `makes at most five requests`() =
        runBlocking {
            val addresses = (0..7).map { "https://maps.app.goo.gl/a$it" }

            fun follower() = RedirectFollower(FakeRedirectSource(chain(*addresses.toTypedArray())))

            // a5 is learnt from the 5th request; a6 would need a 6th.
            assertEquals(addresses[5], follower().follow(addresses[0]) { it == addresses[5] })
            assertNull(follower().follow(addresses[0]) { it == addresses[6] })
        }

    @Test
    fun `counts its requests, not its addresses`() =
        runBlocking {
            val addresses = (0..7).map { "https://maps.app.goo.gl/a$it" }
            val source = FakeRedirectSource(chain(*addresses.toTypedArray()))

            assertNull(RedirectFollower(source).follow(addresses[0]) { false })

            assertEquals(addresses.take(5), source.requested)
        }

    @Test
    fun `gives up when the whole chain takes longer than the timeout`() =
        runBlocking {
            val found = RedirectFollower(NeverAnswers(), timeoutMillis = 50).follow(startOfChain) { false }

            assertNull(found)
        }

    @Test
    fun `stops at an address it has already seen`() =
        runBlocking {
            val loop = "https://maps.app.goo.gl/a1"
            val source = FakeRedirectSource(chain(startOfChain, loop, startOfChain))

            assertNull(RedirectFollower(source).follow(startOfChain) { false })

            assertEquals(listOf(startOfChain, loop), source.requested)
        }

    @Test
    fun `makes no request for an address that is not https`() =
        runBlocking {
            val source = FakeRedirectSource(chain(startOfChain, "http://maps.app.goo.gl/a1", "https://www.google.com/maps/place/X"))

            assertNull(RedirectFollower(source).follow(startOfChain, isMapsPlace))

            assertEquals(listOf(startOfChain), source.requested)
        }

    @Test
    fun `makes no request for a host that is not Google's own`() =
        runBlocking {
            for (stranger in listOf("https://example.com/x", "https://google.com.evil.example/x", "https://bit.ly/x")) {
                val source = FakeRedirectSource(chain(startOfChain, stranger, "https://www.google.com/maps/place/X"))

                assertNull(RedirectFollower(source).follow(startOfChain, isMapsPlace), stranger)

                assertEquals(listOf(startOfChain), source.requested, stranger)
            }
        }

    @Test
    fun `makes no request when the start address may not be requested`() =
        runBlocking {
            val source = FakeRedirectSource(emptyMap())

            assertNull(RedirectFollower(source).follow("http://maps.app.goo.gl/a0") { false })
            assertNull(RedirectFollower(source).follow("https://example.com/a0") { false })

            assertEquals(emptyList<String>(), source.requested)
        }

    @Test
    fun `visits every one of Google's own hosts`() =
        runBlocking {
            val hosts = listOf("maps.app.goo.gl", "goo.gl", "google.com", "www.google.com", "maps.google.com", "consent.google.com")
            for (host in hosts) {
                val source = FakeRedirectSource(emptyMap())

                RedirectFollower(source).follow("https://$host/x") { false }

                assertEquals(listOf("https://$host/x"), source.requested, host)
            }
        }

    @Test
    fun `an error ends the chain`() =
        runBlocking {
            val source = FakeRedirectSource(mapOf(startOfChain to Result.Error(DataError.Network.NO_INTERNET)))

            assertNull(RedirectFollower(source).follow(startOfChain) { false })
        }

    @Test
    fun `an answer with no location ends the chain`() =
        runBlocking {
            val source = FakeRedirectSource(mapOf(startOfChain to Result.Success(null)))

            assertNull(RedirectFollower(source).follow(startOfChain) { false })
            assertEquals(listOf(startOfChain), source.requested)
        }
}
