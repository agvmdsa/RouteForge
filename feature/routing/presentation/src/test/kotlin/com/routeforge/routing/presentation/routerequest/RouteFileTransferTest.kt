package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.RouteFileCodec
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFailure
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.usecase.ExportRouteFileUseCase
import com.routeforge.routing.domain.usecase.ImportRouteFileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private class FakeJsonCodec(
    var parseResult: Result<RouteDraft, RouteFileFailure> = Result.Success(RouteDraft()),
) : RouteFileCodec {
    override fun parse(bytes: ByteArray): Result<RouteDraft, RouteFileFailure> = parseResult

    override fun serialize(route: Route): ByteArray = byteArrayOf(1, 2, 3)
}

class RouteFileTransferTest {
    private val codec = FakeJsonCodec()
    private val codecs = mapOf(RouteFileFormat.JSON to codec)
    private val draftWaypointsHolder = DraftWaypointsHolder()
    private val transfer =
        RouteFileTransfer(
            importRouteFile = ImportRouteFileUseCase(codecs),
            exportRouteFile = ExportRouteFileUseCase(codecs),
            draftWaypointsHolder = draftWaypointsHolder,
        )

    private val placed = RouteDraft(points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)))
    private val someRoute =
        Route(
            points = placed.points,
            geometry = listOf(LatLng(1.0, 1.0), LatLng(2.0, 2.0)),
            distanceMeters = 100.0,
        )

    @Test
    fun `a successful import replaces the draft, clears route, mode and error and writes the draft holder`() {
        val imported = RouteDraft(points = listOf(RoutePoint(9.0, 9.0), RoutePoint(8.0, 8.0)))
        codec.parseResult = Result.Success(imported)
        val state =
            MutableStateFlow(
                RouteRequestState(
                    draft = placed,
                    route = someRoute,
                    chosenMode = RoutePlaybackMode.GUIDED,
                    errorType = RouteRequestError.TOO_FEW_WAYPOINTS_TO_PLAY,
                ),
            )

        transfer.importRoute(byteArrayOf(0), RouteFileFormat.JSON, state)

        assertEquals(imported.points, state.value.draft.points)
        assertNull(state.value.route)
        assertNull(state.value.chosenMode)
        assertNull(state.value.errorType)
        assertEquals(imported.points, draftWaypointsHolder.points.value)
    }

    @Test
    fun `a file with too few waypoints shows its error and leaves the draft untouched`() {
        codec.parseResult = Result.Error(RouteFileFailure.TooFewWaypoints)
        val state = MutableStateFlow(RouteRequestState(draft = placed))

        transfer.importRoute(byteArrayOf(0), RouteFileFormat.JSON, state)

        assertEquals(RouteRequestError.IMPORT_TOO_FEW_WAYPOINTS, state.value.errorType)
        assertEquals(placed.points, state.value.draft.points)
    }

    @Test
    fun `a malformed file shows its error and leaves the draft and the route untouched`() {
        codec.parseResult = Result.Error(RouteFileFailure.Malformed)
        val state = MutableStateFlow(RouteRequestState(draft = placed, route = someRoute))

        transfer.importRoute(byteArrayOf(0), RouteFileFormat.JSON, state)

        assertEquals(RouteRequestError.IMPORT_MALFORMED, state.value.errorType)
        assertEquals(placed.points, state.value.draft.points)
        assertEquals(someRoute, state.value.route)
    }

    @Test
    fun `a coordinate out of range shows its error`() {
        codec.parseResult = Result.Error(RouteFileFailure.CoordinateOutOfRange(RoutePoint(200.0, 0.0)))
        val state = MutableStateFlow(RouteRequestState(draft = placed))

        transfer.importRoute(byteArrayOf(0), RouteFileFormat.JSON, state)

        assertEquals(RouteRequestError.IMPORT_COORDINATE_OUT_OF_RANGE, state.value.errorType)
    }

    @Test
    fun `exporting a computed route serializes it through the codec of the format`() {
        val state = MutableStateFlow(RouteRequestState(draft = placed, route = someRoute))

        val bytes = transfer.exportRoute(RouteFileFormat.JSON, state)

        assertArrayEquals(byteArrayOf(1, 2, 3), bytes)
    }

    @Test
    fun `exporting without a computed route gives nothing`() {
        val state = MutableStateFlow(RouteRequestState(draft = placed))

        assertNull(transfer.exportRoute(RouteFileFormat.JSON, state))
    }
}
