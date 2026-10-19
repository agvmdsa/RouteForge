package com.routeforge.coredomain.link

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResolveSharedTextUseCaseTest {
    private class FakeRedirectSource(
        private val answers: Map<String, Result<String?, DataError.Network>> = emptyMap(),
    ) : RedirectSource {
        val requested = mutableListOf<String>()
        var lookingUpCalledBeforeFirstRequest: Boolean? = null
        var onFirstRequest: () -> Boolean = { true }

        override suspend fun locationOf(url: String): Result<String?, DataError.Network> {
            if (requested.isEmpty()) lookingUpCalledBeforeFirstRequest = onFirstRequest()
            requested += url
            return answers[url] ?: Result.Error(DataError.Network.NO_INTERNET)
        }
    }

    private val shortLink = "https://maps.app.goo.gl/AbCdEf123"
    private val resolved = "https://www.google.com/maps/place/Brandenburg+Gate/@52.5163,13.3777,17z/data=!3d52.5162746!4d13.3777041"

    private fun resolve(
        text: String,
        source: FakeRedirectSource = FakeRedirectSource(),
        onLookingUp: () -> Unit = {},
    ): SharedTextOutcome = runBlocking { ResolveSharedTextUseCase(RedirectFollower(source))(text, onLookingUp) }

    private fun place(
        latitude: Double,
        longitude: Double,
        name: String? = null,
    ) = SharedTextOutcome.Place(RoutePoint(latitude, longitude), name)

    @Test
    fun `an address with a position is read offline`() {
        val source = FakeRedirectSource()
        var lookedUp = 0

        val outcome = resolve("Meet me at geo:52.52,13.405 ok?", source) { lookedUp++ }

        assertEquals(place(52.52, 13.405), outcome)
        assertEquals(emptyList<String>(), source.requested)
        assertEquals(0, lookedUp)
    }

    @Test
    fun `the first address that can be read wins, even after a short link`() {
        val source = FakeRedirectSource()

        val outcome = resolve("$shortLink\ngeo:1,2\ngeo:3,4", source)

        assertEquals(place(1.0, 2.0), outcome)
        assertEquals(emptyList<String>(), source.requested)
    }

    @Test
    fun `a place without a name takes the name from the text, a named one keeps its own`() {
        assertEquals(place(52.52, 13.405, "Cafe Central"), resolve("Cafe Central\ngeo:52.52,13.405"))
        assertEquals(
            place(52.52, 13.405, "Brandenburg Gate"),
            resolve("Something else\ngeo:0,0?q=52.52,13.405(Brandenburg%20Gate)"),
        )
    }

    @Test
    fun `a short link is followed once, after saying so, and its place is returned`() {
        val source = FakeRedirectSource(mapOf(shortLink to Result.Success(resolved)))
        var lookedUp = 0
        source.onFirstRequest = { lookedUp == 1 }

        val outcome = resolve("Brandenburg Gate\n$shortLink", source) { lookedUp++ }

        assertEquals(place(52.5162746, 13.3777041, "Brandenburg Gate"), outcome)
        assertEquals(listOf(shortLink), source.requested)
        assertEquals(1, lookedUp)
        assertEquals(true, source.lookingUpCalledBeforeFirstRequest)
    }

    @Test
    fun `a short link that leads to a nameless place takes the name from the text`() {
        val nameless = "https://www.google.com/maps/@52.52,13.405,15z"
        val source = FakeRedirectSource(mapOf(shortLink to Result.Success(nameless)))

        assertEquals(place(52.52, 13.405, "Cafe Central"), resolve("Cafe Central\n$shortLink", source))
    }

    @Test
    fun `a short link that cannot be followed falls back to a search for the text around it`() {
        val source = FakeRedirectSource()

        val outcome = resolve("Brandenburg Gate\nPariser Platz, 10117 Berlin\n$shortLink", source)

        assertEquals(SharedTextOutcome.Search("Brandenburg Gate Pariser Platz, 10117 Berlin"), outcome)
        assertEquals(listOf(shortLink), source.requested)
    }

    @Test
    fun `a text with no link at all is searched for as it is`() {
        val source = FakeRedirectSource()

        assertEquals(SharedTextOutcome.Search("Cafe Central, Vienna"), resolve("Cafe Central, Vienna", source))
        assertEquals(emptyList<String>(), source.requested)
    }

    @Test
    fun `a short link that cannot be followed and has no text around it is not found`() {
        assertEquals(SharedTextOutcome.NotFound, resolve(shortLink))
    }

    @Test
    fun `a link with no position and no other text is not found`() {
        val source = FakeRedirectSource()

        assertEquals(SharedTextOutcome.NotFound, resolve("https://www.openstreetmap.org/node/123456", source))
        assertEquals(SharedTextOutcome.NotFound, resolve("   ", source))
        assertEquals(SharedTextOutcome.NotFound, resolve("", source))
        assertEquals(emptyList<String>(), source.requested)
    }

    @Test
    fun `a link to an unsupported site next to a line of text searches for that line`() {
        val outcome = resolve("Nice spot\nhttps://example.com/place/1")

        assertEquals(SharedTextOutcome.Search("Nice spot"), outcome)
    }

    @Test
    fun `a directions link is read offline into a route`() {
        val source = FakeRedirectSource()

        val outcome = resolve("Route\nhttps://www.google.com/maps/dir/52.51,13.37/52.52,13.41/", source)

        assertEquals(SharedTextOutcome.Route(listOf(RoutePoint(52.51, 13.37), RoutePoint(52.52, 13.41))), outcome)
        assertEquals(emptyList<String>(), source.requested)
    }

    @Test
    fun `a short link that leads to a directions link is a route`() {
        val directions = "https://www.google.com/maps/dir/52.51,13.37/52.52,13.41/"
        val source = FakeRedirectSource(mapOf(shortLink to Result.Success(directions)))

        val outcome = resolve(shortLink, source)

        assertEquals(SharedTextOutcome.Route(listOf(RoutePoint(52.51, 13.37), RoutePoint(52.52, 13.41))), outcome)
    }

    @Test
    fun `the first address that can be read wins, whether it is a route or a place`() {
        val directions = "https://www.google.com/maps/dir/52.51,13.37/52.52,13.41/"

        assertEquals(
            SharedTextOutcome.Route(listOf(RoutePoint(52.51, 13.37), RoutePoint(52.52, 13.41))),
            resolve("$directions\ngeo:1,2"),
        )
        assertEquals(place(1.0, 2.0), resolve("geo:1,2\n$directions"))
    }
}
