package com.routeforge.coredomain.link

import com.routeforge.coredomain.model.RoutePoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ExternalLinkParserTest {
    private fun place(
        latitude: Double,
        longitude: Double,
        name: String? = null,
    ) = ExternalLink.Place(RoutePoint(latitude, longitude), name)

    private fun route(vararg stops: Pair<Double, Double>) = ExternalLink.Route(stops.map { RoutePoint(it.first, it.second) })

    private fun check(
        expected: ExternalLink,
        address: String,
    ) = assertEquals(expected, ExternalLinkParser.parse(address), address)

    private fun assertNone(vararg addresses: String) {
        for (address in addresses) check(ExternalLink.None, address)
    }

    @Test
    fun `a geo link with a position`() {
        check(place(52.52, 13.405), "geo:52.5200,13.4050")
        check(place(-8.05, -34.9), "geo:-8.05,-34.9")
    }

    @Test
    fun `altitude, zoom and uncertainty in a geo link are ignored`() {
        check(place(52.52, 13.405), "geo:52.52,13.405,34?z=15")
        check(place(52.52, 13.405), "geo:52.52,13.405;u=35")
        check(place(52.52, 13.405), "geo:52.52,13.405;crs=wgs84;u=35?z=12")
    }

    @Test
    fun `a geo link may keep its position in the query, with an optional label`() {
        check(place(52.52, 13.405), "geo:0,0?q=52.52,13.405")
        check(place(52.52, 13.405, "Brandenburg Gate"), "geo:0,0?q=52.52,13.405(Brandenburg%20Gate)")
        check(place(52.52, 13.405, "Café Central"), "geo:0,0?q=52.52,13.405(Caf%C3%A9+Central)")
    }

    @Test
    fun `a position in the path of a geo link wins, and a plain query is only a search text`() {
        check(place(52.52, 13.405), "geo:52.52,13.405?q=Cafe")
        check(place(52.52, 13.405, "Spot"), "geo:52.52,13.405?q=52.52,13.405(Spot)")
    }

    @Test
    fun `a geo link with no usable position`() {
        assertNone("geo:0,0?q=Cafe", "geo:0,0", "geo:", "geo:abc,def", "geo:91,13", "geo:52,181", "geo:-91,0", "geo:0,-181")
    }

    @Test
    fun `scheme and host are not case sensitive`() {
        check(place(52.5, 13.4), "GEO:52.5,13.4")
        check(place(52.52, 13.405), "HTTPS://WWW.OPENSTREETMAP.ORG/?mlat=52.52&mlon=13.405")
    }

    @Test
    fun `an OpenStreetMap link with a marker`() {
        check(place(52.52, 13.405), "https://www.openstreetmap.org/?mlat=52.52&mlon=13.405")
        check(place(52.52, 13.405), "https://openstreetmap.org/?mlon=13.405&mlat=52.52")
        check(place(52.52, 13.405), "http://www.openstreetmap.org/?mlat=52.52&mlon=13.405&zoom=16")
    }

    @Test
    fun `an OpenStreetMap marker wins over the map centre`() {
        check(place(52.52, 13.405), "https://www.openstreetmap.org/?mlat=52.52&mlon=13.405#map=16/52.50/13.40")
    }

    @Test
    fun `an OpenStreetMap link with only a map centre uses the centre`() {
        check(place(52.52, 13.405), "https://www.openstreetmap.org/#map=16/52.52/13.405")
        check(place(52.52, 13.405), "https://www.openstreetmap.org/#map=16/52.52/13.405&layers=N")
    }

    @Test
    fun `an OpenStreetMap link with no position`() {
        assertNone(
            "https://www.openstreetmap.org/node/123456",
            "https://www.openstreetmap.org/",
            "https://www.openstreetmap.org/?mlat=91&mlon=13",
            "https://www.openstreetmap.org/?mlat=52.52",
            "https://osm.org/go/xyz",
        )
    }

    @Test
    fun `a Google Maps place link takes the pin, not the view centre, and the place name`() {
        val link =
            "https://www.google.com/maps/place/Brandenburg+Gate/@52.5163,13.3777,17z/" +
                "data=!3m1!4b1!4m6!3m5!1s0x1:0x2!8m2!3d52.5162746!4d13.3777041"
        check(place(52.5162746, 13.3777041, "Brandenburg Gate"), link)
    }

    @Test
    fun `a Google Maps place name is decoded`() {
        check(place(48.2, 16.3, "Café Central"), "https://www.google.com/maps/place/Caf%C3%A9+Central/@48.2,16.3,17z")
    }

    @Test
    fun `a Google Maps link with only a view centre uses the centre`() {
        check(place(52.52, 13.405, "Berlin"), "https://www.google.com/maps/place/Berlin/@52.52,13.405,12z")
        check(place(52.52, 13.405), "https://www.google.com/maps/@52.52,13.405,15z")
        check(place(52.52, 13.405), "https://www.google.com/maps/search/pizza/@52.52,13.405,13z")
    }

    @Test
    fun `a Google Maps link with the position in the query`() {
        check(place(52.52, 13.405), "https://www.google.com/maps?q=52.52,13.405")
        check(place(52.52, 13.405), "https://www.google.com/maps?q=loc:52.52,13.405")
        check(place(52.52, 13.405), "https://maps.google.com/?ll=52.52,13.405&z=14")
        check(place(52.52, 13.405), "https://maps.google.com/?q=52.52,13.405")
        check(place(52.52, 13.405), "https://www.google.com/maps/search/?api=1&query=52.52,13.405")
    }

    @Test
    fun `a Google Maps link with no position`() {
        assertNone(
            "https://www.google.com/maps/search/?api=1&query=Cafe+Berlin",
            "https://www.google.com/maps?q=Cafe",
            "https://www.google.com/maps/@91,13,15z",
            "https://www.google.com/search?q=52.52,13.405",
            "https://www.google.com/",
        )
    }

    @Test
    fun `a directions link is never read as a place, whatever view centre it carries`() {
        val address = "https://www.google.com/maps/dir/Brandenburg+Gate/Alexanderplatz/@52.52,13.40,14z"

        assertFalse(ExternalLinkParser.parse(address) is ExternalLink.Place)
    }

    @Test
    fun `a directions link with coordinate stops is a route in the order of the stops`() {
        check(route(52.51 to 13.37, 52.52 to 13.41), "https://www.google.com/maps/dir/52.51,13.37/52.52,13.41/")
        check(
            route(52.51 to 13.37, 52.52 to 13.41, 52.53 to 13.45),
            "https://www.google.com/maps/dir/52.51,13.37/52.52,13.41/52.53,13.45/@52.52,13.40,13z",
        )
    }

    @Test
    fun `a directions link with names only takes its stops from the encoded pairs, longitude first`() {
        val address =
            "https://www.google.com/maps/dir/Brandenburg+Gate/Alexanderplatz/" +
                "data=!4m14!4m13!1m5!1m1!1s0x1:0x2!2m2!1d13.3777!2d52.5163!1m5!1m1!1s0x3:0x4!2m2!1d13.4132!2d52.5219!3e0"

        check(route(52.5163 to 13.3777, 52.5219 to 13.4132), address)
    }

    @Test
    fun `a stop that is only a name is skipped, never looked up`() {
        check(
            route(52.51 to 13.37, 52.53 to 13.45),
            "https://www.google.com/maps/dir/52.51,13.37/Alexanderplatz/52.53,13.45/",
        )
    }

    @Test
    fun `a directions link with fewer than two stops that have coordinates is not a route`() {
        assertNone(
            "https://www.google.com/maps/dir/52.51,13.37/Alexanderplatz/",
            "https://www.google.com/maps/dir//52.52,13.41/",
            "https://www.google.com/maps/dir/Brandenburg+Gate/Alexanderplatz/@52.52,13.40,14z/data=!4m8!4m7!1m0!1m0!3e0",
            "https://www.google.com/maps/dir/",
            "https://www.google.com/maps/dir/52.51,13.37/91,13/",
        )
    }

    @Test
    fun `the view centre of a directions link is never read as a stop or a place`() {
        val address = "https://www.google.com/maps/dir/52.51,13.37/Alexanderplatz/@52.52,13.40,14z"

        assertFalse(ExternalLinkParser.parse(address) is ExternalLink.Place)
        assertFalse(ExternalLinkParser.parse(address) is ExternalLink.Route)
    }

    @Test
    fun `a short Google Maps link is recognised and left alone`() {
        check(ExternalLink.ShortLink("https://maps.app.goo.gl/AbCdEf123"), "https://maps.app.goo.gl/AbCdEf123")
        check(ExternalLink.ShortLink("https://goo.gl/maps/AbCdEf123"), "https://goo.gl/maps/AbCdEf123")
        check(ExternalLink.ShortLink("http://maps.app.goo.gl/AbCdEf123"), "  http://maps.app.goo.gl/AbCdEf123 ")
    }

    @Test
    fun `other shorteners and other goo gl paths are not short Google Maps links`() {
        assertNone("https://goo.gl/photos/AbCdEf123", "https://goo.gl/", "https://bit.ly/AbCdEf123", "https://g.co/kgs/AbCdEf")
    }

    @Test
    fun `other hosts, schemes and shapes are not read`() {
        assertNone(
            "https://example.com/maps/@52.52,13.405,15z",
            "https://www.google.com.br/maps/@52.52,13.405,15z",
            "ftp://www.openstreetmap.org/?mlat=52.52&mlon=13.405",
            "mailto:someone@example.com",
            "https://www.bing.com/maps?cp=52.52~13.405",
            "",
            "   ",
            "garbage",
            "52.52,13.405",
        )
    }

    @Test
    fun `whitespace around the address does not matter`() {
        check(place(52.52, 13.405), "  geo:52.52,13.405\n")
    }

    @Test
    fun `parsing never throws`() {
        for (address in listOf("geo:%", "https://", "https://www.google.com/maps/place//@,,,", "geo:1e999,1e999", "geo:NaN,NaN", "%%%%")) {
            ExternalLinkParser.parse(address)
        }
    }
}
