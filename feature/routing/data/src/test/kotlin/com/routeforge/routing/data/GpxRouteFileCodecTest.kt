package com.routeforge.routing.data

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.model.RouteFileFailure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class GpxRouteFileCodecTest {
    private val codec = GpxRouteFileCodec()

    @Test
    fun `parses trkpt track points`() {
        val gpx =
            """
            <?xml version="1.0"?>
            <gpx><trk><trkseg>
              <trkpt lat="-8.0476" lon="-34.8770"/>
              <trkpt lat="-8.0512" lon="-34.8801"/>
            </trkseg></trk></gpx>
            """.trimIndent().toByteArray()

        val result = codec.parse(gpx)

        assertEquals(
            listOf(RoutePoint(-8.0476, -34.8770), RoutePoint(-8.0512, -34.8801)),
            (result as Result.Success).data.points,
        )
    }

    @Test
    fun `parses rtept and wpt point tags as well`() {
        val gpx =
            """
            <?xml version="1.0"?>
            <gpx>
              <wpt lat="1.0" lon="1.0"/>
              <rte><rtept lat="2.0" lon="2.0"/></rte>
            </gpx>
            """.trimIndent().toByteArray()

        val result = codec.parse(gpx)

        assertEquals(listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)), (result as Result.Success).data.points)
    }

    @Test
    fun `rejects a file with fewer than two waypoints`() {
        val gpx = """<gpx><trkpt lat="1.0" lon="1.0"/></gpx>""".toByteArray()

        val result = codec.parse(gpx)

        assertEquals(Result.Error(RouteFileFailure.TooFewWaypoints), result)
    }

    @Test
    fun `rejects a coordinate outside valid longitude range`() {
        val gpx =
            """<gpx><trkpt lat="1.0" lon="200.0"/><trkpt lat="2.0" lon="2.0"/></gpx>""".toByteArray()

        val result = codec.parse(gpx)

        assertInstanceOf(Result.Error::class.java, result)
        assertInstanceOf(RouteFileFailure.CoordinateOutOfRange::class.java, (result as Result.Error).error)
    }

    @Test
    fun `rejects a structurally malformed file`() {
        val gpx = "<gpx><trkpt lat=".toByteArray()

        val result = codec.parse(gpx)

        assertEquals(Result.Error(RouteFileFailure.Malformed), result)
    }

    @Test
    fun `exporting and re-importing a route produces identical waypoints`() {
        val route =
            Route(
                points = listOf(RoutePoint(-8.0476, -34.8770), RoutePoint(-8.0512, -34.8801)),
                geometry = emptyList(),
                distanceMeters = 0.0,
            )

        val exported = codec.serialize(route)
        val reimported = codec.parse(exported)

        assertEquals(route.points, (reimported as Result.Success).data.points)
    }
}
