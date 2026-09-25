package com.routeforge.routing.data

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.model.RouteFileFailure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class JsonRouteFileCodecTest {
    private val codec = JsonRouteFileCodec()

    @Test
    fun `parses a valid route with two or more waypoints`() {
        val bytes =
            """{"name":"Rota","waypoints":[{"latitude":-8.0476,"longitude":-34.8770},{"latitude":-8.0512,"longitude":-34.8801}]}"""
                .toByteArray()

        val result = codec.parse(bytes)

        assertEquals(
            listOf(RoutePoint(-8.0476, -34.8770), RoutePoint(-8.0512, -34.8801)),
            (result as Result.Success).data.points,
        )
    }

    @Test
    fun `rejects a file with fewer than two waypoints`() {
        val bytes = """{"waypoints":[{"latitude":1.0,"longitude":1.0}]}""".toByteArray()

        val result = codec.parse(bytes)

        assertEquals(Result.Error(RouteFileFailure.TooFewWaypoints), result)
    }

    @Test
    fun `rejects a coordinate outside valid latitude range`() {
        val bytes = """{"waypoints":[{"latitude":200.0,"longitude":1.0},{"latitude":1.0,"longitude":1.0}]}""".toByteArray()

        val result = codec.parse(bytes)

        assertInstanceOf(Result.Error::class.java, result)
        assertInstanceOf(RouteFileFailure.CoordinateOutOfRange::class.java, (result as Result.Error).error)
    }

    @Test
    fun `rejects a structurally malformed file`() {
        val bytes = "not json at all {{{".toByteArray()

        val result = codec.parse(bytes)

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
