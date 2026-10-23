package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.coredomain.holder.PendingIncomingRouteHolder
import com.routeforge.coredomain.holder.SelectedFavoriteWaypointHolder
import com.routeforge.coredomain.model.IncomingRoute
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.testing.FakeRegionCatalog
import com.routeforge.coredomain.testing.FakeRoutingEngine
import com.routeforge.routing.domain.RouteFileCodec
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFailure
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.usecase.ImportRouteFileUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private class FakeGpxCodec(
    var parseResult: Result<RouteDraft, RouteFileFailure> = Result.Success(RouteDraft()),
) : RouteFileCodec {
    override fun parse(bytes: ByteArray): Result<RouteDraft, RouteFileFailure> = parseResult

    override fun serialize(route: Route): ByteArray = byteArrayOf()
}

class IncomingRouteReceiverTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val gpxCodec = FakeGpxCodec()
    private val draftWaypointsHolder = DraftWaypointsHolder()
    private val pendingIncomingRouteHolder = PendingIncomingRouteHolder()
    private val receiver =
        IncomingRouteReceiver(
            importRouteFile = ImportRouteFileUseCase(mapOf(RouteFileFormat.GPX to gpxCodec)),
            pendingIncomingRouteHolder = pendingIncomingRouteHolder,
            routeDraftEditor =
                RouteDraftEditor(
                    draftWaypointsHolder = draftWaypointsHolder,
                    selectedFavoriteWaypointHolder = SelectedFavoriteWaypointHolder(),
                    routeModePreviewer =
                        newRouteModePreviewer(
                            FakeRoutingEngine(snappableLatitudes = emptySet()),
                            FakeRegionCatalog(),
                            dispatcher,
                        ),
                ),
        )

    private val arrivedPoints = listOf(RoutePoint(9.0, 9.0), RoutePoint(8.0, 8.0))
    private val placed = RouteDraft(points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)))

    private fun arrivingFile(points: List<RoutePoint> = arrivedPoints) {
        gpxCodec.parseResult = Result.Success(RouteDraft(points = points))
        pendingIncomingRouteHolder.set(IncomingRoute.GpxFile(byteArrayOf(0)))
    }

    @Test
    fun `a file that arrives while nothing is placed becomes the draft and the holder is cleared`() {
        val state = MutableStateFlow(RouteRequestState())
        receiver.startObservingIncomingRoute(state, scope)

        arrivingFile()

        assertEquals(arrivedPoints, state.value.draft.points)
        assertEquals(arrivedPoints, draftWaypointsHolder.points.value)
        assertNull(state.value.pendingReplaceDraft)
        assertNull(pendingIncomingRouteHolder.route.value)
    }

    @Test
    fun `the stops of a directions link follow the same rules as a file`() {
        val state = MutableStateFlow(RouteRequestState())
        receiver.startObservingIncomingRoute(state, scope)

        pendingIncomingRouteHolder.set(IncomingRoute.Stops(arrivedPoints))

        assertEquals(arrivedPoints, state.value.draft.points)
    }

    @Test
    fun `a route that was already waiting when the screen started is taken too`() {
        arrivingFile()
        val state = MutableStateFlow(RouteRequestState())

        receiver.startObservingIncomingRoute(state, scope)

        assertEquals(arrivedPoints, state.value.draft.points)
    }

    @Test
    fun `a file that arrives while waypoints are placed asks first and leaves the draft alone`() {
        val state = MutableStateFlow(RouteRequestState(draft = placed))
        receiver.startObservingIncomingRoute(state, scope)

        arrivingFile()

        assertEquals(placed.points, state.value.draft.points)
        assertEquals(arrivedPoints, state.value.pendingReplaceDraft?.points)
    }

    @Test
    fun `a second route that arrives during the question replaces the one being asked about`() {
        val state = MutableStateFlow(RouteRequestState(draft = placed))
        receiver.startObservingIncomingRoute(state, scope)
        arrivingFile()
        val later = listOf(RoutePoint(5.0, 5.0), RoutePoint(6.0, 6.0), RoutePoint(7.0, 7.0))

        arrivingFile(later)

        assertEquals(later, state.value.pendingReplaceDraft?.points)
    }

    @Test
    fun `confirming the question replaces the draft and ends the question`() {
        val state = MutableStateFlow(RouteRequestState(draft = placed, pendingReplaceDraft = RouteDraft(points = arrivedPoints)))

        receiver.confirmReplaceDraft(state, scope)

        assertEquals(arrivedPoints, state.value.draft.points)
        assertEquals(arrivedPoints, draftWaypointsHolder.points.value)
        assertNull(state.value.pendingReplaceDraft)
    }

    @Test
    fun `answering when no question is open changes nothing`() {
        val state = MutableStateFlow(RouteRequestState(draft = placed))

        receiver.confirmReplaceDraft(state, scope)

        assertEquals(placed.points, state.value.draft.points)
        assertNull(state.value.pendingReplaceDraft)
    }

    @Test
    fun `a malformed file shows the import error and leaves the draft alone`() {
        val state = MutableStateFlow(RouteRequestState(draft = placed))
        receiver.startObservingIncomingRoute(state, scope)
        gpxCodec.parseResult = Result.Error(RouteFileFailure.Malformed)

        pendingIncomingRouteHolder.set(IncomingRoute.GpxFile(byteArrayOf(0)))

        assertEquals(RouteRequestError.IMPORT_MALFORMED, state.value.errorType)
        assertEquals(placed.points, state.value.draft.points)
        assertNull(state.value.pendingReplaceDraft)
    }

    @Test
    fun `a route with fewer than two points shows the too-few-waypoints error`() {
        val state = MutableStateFlow(RouteRequestState())
        receiver.startObservingIncomingRoute(state, scope)

        pendingIncomingRouteHolder.set(IncomingRoute.Stops(listOf(RoutePoint(1.0, 1.0))))

        assertEquals(RouteRequestError.IMPORT_TOO_FEW_WAYPOINTS, state.value.errorType)
        assertEquals(emptyList<RoutePoint>(), state.value.draft.points)
    }
}
