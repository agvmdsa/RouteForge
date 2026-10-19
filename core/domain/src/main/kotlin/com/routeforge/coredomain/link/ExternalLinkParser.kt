package com.routeforge.coredomain.link

import com.routeforge.coredomain.model.RoutePoint
import java.net.URLDecoder

private val NUMBER = """-?\d+(?:\.\d+)?"""
private val WEB_ADDRESS = Regex("""^(https?)://([^/?#]+)([^?#]*)(?:\?([^#]*))?(?:#(.*))?$""", RegexOption.IGNORE_CASE)
private val COORDINATE_PAIR = Regex("""^\s*($NUMBER)\s*,\s*($NUMBER)\s*(?:\((.*)\))?\s*$""")
private val OSM_MAP_FRAGMENT = Regex("""map=\d+(?:\.\d+)?/($NUMBER)/($NUMBER)""")
private val GOOGLE_PIN = Regex("""!3d($NUMBER)!4d($NUMBER)""")
private val GOOGLE_VIEW_CENTRE = Regex("""/@($NUMBER),($NUMBER)""")
private val GOOGLE_PLACE_NAME = Regex("""/maps/place/([^/]+)""")

private val OSM_HOSTS = setOf("openstreetmap.org", "www.openstreetmap.org")
private val GOOGLE_HOSTS = setOf("google.com", "www.google.com", "maps.google.com")
private val GOOGLE_POSITION_PARAMETERS = listOf("q", "ll", "query")

/** Reads what a link means, in plain Kotlin so that it can be tested without Android. Never throws. */
object ExternalLinkParser {
    fun parse(address: String): ExternalLink = runCatching { read(address.trim()) }.getOrDefault(ExternalLink.None)

    private fun read(address: String): ExternalLink =
        when {
            address.startsWith("geo:", ignoreCase = true) -> readGeo(address.substring("geo:".length))
            else -> readWebAddress(address)
        }

    private fun readGeo(rest: String): ExternalLink {
        val coordinates = rest.substringBefore('?').substringBefore(';').split(',').map { it.trim() }
        // A third value is the altitude, which is ignored.
        val inPath = if (coordinates.size >= 2) pointOf(coordinates[0], coordinates[1]) else null
        val fromQuery = queryParameters(rest.substringAfter('?', ""))["q"]?.let { COORDINATE_PAIR.matchEntire(it) }
        val label = fromQuery?.groupValues?.get(3)?.takeIf { it.isNotBlank() }
        return when {
            inPath == null -> ExternalLink.None
            !isNowhere(inPath) -> ExternalLink.Place(inPath, label)
            else -> fromQuery?.let { pointOf(it) }?.takeIf { !isNowhere(it) }?.let { ExternalLink.Place(it, label) } ?: ExternalLink.None
        }
    }

    private fun readWebAddress(address: String): ExternalLink {
        val match = WEB_ADDRESS.matchEntire(address) ?: return ExternalLink.None
        val host = match.groupValues[2].lowercase()
        val path = match.groupValues[3]
        val query = queryParameters(match.groupValues[4])
        val fragment = match.groupValues[5]
        return when (host) {
            in OSM_HOSTS -> readOpenStreetMap(query, fragment)
            in GOOGLE_HOSTS -> if (isMapsPath(host, path)) readGoogleMaps(address, path, query) else ExternalLink.None
            else -> ExternalLink.None
        }
    }

    private fun isMapsPath(
        host: String,
        path: String,
    ) = host == "maps.google.com" || path.startsWith("/maps")

    private fun readOpenStreetMap(
        query: Map<String, String>,
        fragment: String,
    ): ExternalLink {
        val marker = pointOf(query["mlat"], query["mlon"])
        val centre = OSM_MAP_FRAGMENT.find(fragment)?.let { pointOf(it) }
        return (marker ?: centre)?.let { ExternalLink.Place(it, name = null) } ?: ExternalLink.None
    }

    private fun readGoogleMaps(
        address: String,
        path: String,
        query: Map<String, String>,
    ): ExternalLink {
        if (path.startsWith("/maps/dir")) return ExternalLink.None
        val pin = GOOGLE_PIN.findAll(address).lastOrNull()?.let { pointOf(it) }
        val fromQuery =
            GOOGLE_POSITION_PARAMETERS.firstNotNullOfOrNull { name ->
                query[name]?.removePrefix("loc:")?.let { COORDINATE_PAIR.matchEntire(it) }?.let { pointOf(it) }
            }
        val centre = GOOGLE_VIEW_CENTRE.find(path)?.let { pointOf(it) }
        val point = pin ?: fromQuery ?: centre ?: return ExternalLink.None
        return ExternalLink.Place(point, googlePlaceName(path))
    }

    private fun googlePlaceName(path: String): String? =
        GOOGLE_PLACE_NAME
            .find(path)
            ?.groupValues
            ?.get(1)
            ?.let { URLDecoder.decode(it, "UTF-8").trim() }
            ?.takeIf { it.isNotBlank() && !COORDINATE_PAIR.matches(it) }

    private fun queryParameters(query: String): Map<String, String> =
        query
            .split('&')
            .filter { it.contains('=') }
            .associate { it.substringBefore('=').lowercase() to URLDecoder.decode(it.substringAfter('='), "UTF-8") }

    private fun pointOf(match: MatchResult): RoutePoint? = pointOf(match.groupValues[1], match.groupValues[2])

    private fun pointOf(
        latitude: String?,
        longitude: String?,
    ): RoutePoint? {
        val lat = latitude?.trim()?.toDoubleOrNull() ?: return null
        val lon = longitude?.trim()?.toDoubleOrNull() ?: return null
        return RoutePoint(lat, lon).takeIf { lat in -90.0..90.0 && lon in -180.0..180.0 }
    }

    /** `geo:0,0` is the idiom for "no position of its own". */
    private fun isNowhere(point: RoutePoint) = point.latitude == 0.0 && point.longitude == 0.0
}
