package com.routeforge.routing.data

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.RouteFileCodec
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFailure

private val LATITUDE_RANGE = -90.0..90.0
private val LONGITUDE_RANGE = -180.0..180.0

// Matches a self-contained trkpt/rtept/wpt element and captures its attribute string, so
// lat/lon can be read regardless of attribute order.
private val POINT_TAG_REGEX = Regex("""<(?:trkpt|rtept|wpt)\b([^>]*)/?>""")
private val POINT_TAG_OPENING_REGEX = Regex("""<(?:trkpt|rtept|wpt)\b""")
private val LATITUDE_ATTRIBUTE_REGEX = Regex("""lat\s*=\s*"([^"]*)"""")
private val LONGITUDE_ATTRIBUTE_REGEX = Regex("""lon\s*=\s*"([^"]*)"""")

/**
 * FR-011–FR-015: GPX route file format. Reads `trkpt`/`rtept`/`wpt` point tags from their
 * `lat`/`lon` attributes, matching the format already documented for FakeGPS-compatible import.
 *
 * Parsing is done with a targeted regex rather than `javax.xml`/SAX: Android's unit-test
 * classpath stubs out `javax.xml.parsers` (no real parser implementation without Robolectric),
 * which made a SAX-based parser fail under `./gradlew test` despite working fine standalone.
 * GPX point tags have a fixed, simple shape, so a regex is both correct and dependency-free here.
 */
class GpxRouteFileCodec : RouteFileCodec {
    override fun parse(bytes: ByteArray): Result<RouteDraft, RouteFileFailure> {
        val text = bytes.decodeToString()
        if (!text.contains("<gpx", ignoreCase = true)) {
            return Result.Error(RouteFileFailure.Malformed)
        }

        val points = mutableListOf<RoutePoint>()
        for (match in POINT_TAG_REGEX.findAll(text)) {
            val attributes = match.groupValues[1]
            val latitude = LATITUDE_ATTRIBUTE_REGEX.find(attributes)?.groupValues?.get(1)?.toDoubleOrNull()
            val longitude = LONGITUDE_ATTRIBUTE_REGEX.find(attributes)?.groupValues?.get(1)?.toDoubleOrNull()
            if (latitude == null || longitude == null) {
                return Result.Error(RouteFileFailure.Malformed)
            }
            if (latitude !in LATITUDE_RANGE || longitude !in LONGITUDE_RANGE) {
                return Result.Error(RouteFileFailure.CoordinateOutOfRange(RoutePoint(latitude, longitude)))
            }
            points.add(RoutePoint(latitude = latitude, longitude = longitude))
        }

        if (points.isEmpty() && POINT_TAG_OPENING_REGEX.containsMatchIn(text)) {
            // A point tag was started but never matched a complete, well-formed element.
            return Result.Error(RouteFileFailure.Malformed)
        }
        if (points.size < 2) return Result.Error(RouteFileFailure.TooFewWaypoints)
        return Result.Success(RouteDraft(points = points))
    }

    override fun serialize(route: Route): ByteArray {
        val trackPoints =
            route.points.joinToString(separator = "\n") { point ->
                "      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\"/>"
            }
        val xml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="RouteForge">
              <trk>
                <trkseg>
            $trackPoints
                </trkseg>
              </trk>
            </gpx>
            """.trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }
}
