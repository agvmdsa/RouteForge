package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.RouteFileCodec
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFailure
import com.routeforge.routing.domain.model.RouteFileFormat
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private class FakeRouteFileCodec(
    private val parseResult: Result<RouteDraft, RouteFileFailure>,
    private val serializeResult: ByteArray = ByteArray(0),
) : RouteFileCodec {
    var lastSerializedRoute: Route? = null

    override fun parse(bytes: ByteArray): Result<RouteDraft, RouteFileFailure> = parseResult

    override fun serialize(route: Route): ByteArray {
        lastSerializedRoute = route
        return serializeResult
    }
}

class RouteFileUseCasesTest {
    private val draft = RouteDraft().add(RoutePoint(1.0, 1.0)).add(RoutePoint(2.0, 2.0))
    private val jsonCodec = FakeRouteFileCodec(parseResult = Result.Success(draft), serializeResult = byteArrayOf(1))
    private val gpxCodec = FakeRouteFileCodec(parseResult = Result.Success(draft), serializeResult = byteArrayOf(2))
    private val codecs = mapOf(RouteFileFormat.JSON to jsonCodec, RouteFileFormat.GPX to gpxCodec)
    private val importUseCase = ImportRouteFileUseCase(codecs)
    private val exportUseCase = ExportRouteFileUseCase(codecs)

    @Test
    fun `import dispatches to the codec matching the requested format`() {
        val result = importUseCase(byteArrayOf(0), RouteFileFormat.JSON)

        assertEquals(Result.Success(draft), result)
    }

    @Test
    fun `export dispatches to the codec matching the requested format`() {
        val route = Route(points = draft.points, geometry = emptyList(), distanceMeters = 0.0)

        val bytes = exportUseCase(route, RouteFileFormat.GPX)

        assertArrayEquals(byteArrayOf(2), bytes)
        assertEquals(route, gpxCodec.lastSerializedRoute)
    }
}
