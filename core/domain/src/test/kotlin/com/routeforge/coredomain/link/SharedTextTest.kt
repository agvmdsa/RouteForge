package com.routeforge.coredomain.link

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SharedTextTest {
    @Test
    fun `a text from the Google Maps share sheet gives its link, its name and its search text`() {
        val shared =
            SharedText.parse(
                "Brandenburg Gate\nPariser Platz, 10117 Berlin\nhttps://maps.app.goo.gl/AbCdEf123",
            )

        assertEquals(listOf("https://maps.app.goo.gl/AbCdEf123"), shared.addresses)
        assertEquals("Brandenburg Gate", shared.name)
        assertEquals("Brandenburg Gate Pariser Platz, 10117 Berlin", shared.searchText)
    }

    @Test
    fun `a text with only a link has neither a name nor a search text`() {
        val shared = SharedText.parse("https://maps.app.goo.gl/AbCdEf123")

        assertEquals(listOf("https://maps.app.goo.gl/AbCdEf123"), shared.addresses)
        assertNull(shared.name)
        assertNull(shared.searchText)
    }

    @Test
    fun `a line that contains a link is neither a name nor search text`() {
        val shared = SharedText.parse("Meet me at geo:52.52,13.405 ok?")

        assertEquals(listOf("geo:52.52,13.405"), shared.addresses)
        assertNull(shared.name)
        assertNull(shared.searchText)
    }

    @Test
    fun `a text with no link is its own name and search text`() {
        val shared = SharedText.parse("Cafe Central, Vienna")

        assertEquals(emptyList<String>(), shared.addresses)
        assertEquals("Cafe Central, Vienna", shared.name)
        assertEquals("Cafe Central, Vienna", shared.searchText)
    }

    @Test
    fun `addresses come in order of appearance, web links and geo links alike`() {
        val shared =
            SharedText.parse(
                "geo:1,2\nsee https://www.openstreetmap.org/?mlat=3&mlon=4 and http://example.com/x\n",
            )

        assertEquals(
            listOf("geo:1,2", "https://www.openstreetmap.org/?mlat=3&mlon=4", "http://example.com/x"),
            shared.addresses,
        )
    }

    @Test
    fun `punctuation right after a link is not part of it`() {
        val shared = SharedText.parse("Look: (https://maps.app.goo.gl/AbCdEf123), or https://goo.gl/maps/Zz9.")

        assertEquals(
            listOf("https://maps.app.goo.gl/AbCdEf123", "https://goo.gl/maps/Zz9"),
            shared.addresses,
        )
    }

    @Test
    fun `the search text is the first three lines without a link`() {
        val shared = SharedText.parse("One\nTwo\nhttps://goo.gl/maps/Zz9\nThree\nFour\nFive")

        assertEquals("One", shared.name)
        assertEquals("One Two Three", shared.searchText)
    }

    @Test
    fun `blank lines are skipped and Windows line endings are understood`() {
        val shared = SharedText.parse("\r\n  One  \r\n\r\n\tTwo\r\nhttps://goo.gl/maps/Zz9\r\n")

        assertEquals(listOf("https://goo.gl/maps/Zz9"), shared.addresses)
        assertEquals("One", shared.name)
        assertEquals("One Two", shared.searchText)
    }

    @Test
    fun `the search text is cut to 200 characters`() {
        val shared = SharedText.parse("a".repeat(150) + "\n" + "b".repeat(150))

        assertEquals(200, shared.searchText?.length)
        assertEquals("a".repeat(150) + " " + "b".repeat(49), shared.searchText)
    }

    @Test
    fun `a closing bracket that closes one inside a geo link is part of it`() {
        val shared = SharedText.parse("Go to geo:0,0?q=52.52,13.405(Brandenburg%20Gate).")

        assertEquals(listOf("geo:0,0?q=52.52,13.405(Brandenburg%20Gate)"), shared.addresses)
    }

    @Test
    fun `a name is cut to 200 characters too`() {
        assertEquals(200, SharedText.parse("n".repeat(500)).name?.length)
    }

    @Test
    fun `an empty or blank text has nothing`() {
        for (text in listOf("", "   ", "\n\r\n\t")) {
            val shared = SharedText.parse(text)

            assertEquals(emptyList<String>(), shared.addresses, text)
            assertNull(shared.name, text)
            assertNull(shared.searchText, text)
        }
    }

    @Test
    fun `only the first 10000 characters are read`() {
        val padding = "x".repeat(10_000) + "\n"
        val shared = SharedText.parse(padding + "https://maps.app.goo.gl/TooLate")

        assertEquals(emptyList<String>(), shared.addresses)
    }

    @Test
    fun `a link inside the first 10000 characters is still found`() {
        val shared = SharedText.parse("https://maps.app.goo.gl/InTime\n" + "x".repeat(20_000))

        assertEquals(listOf("https://maps.app.goo.gl/InTime"), shared.addresses)
    }
}
