package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.model.RouteFileFailure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RouteFileFailureMappingTest {
    @Test
    fun `too few waypoints maps to the too-few-waypoints import error`() {
        assertEquals(RouteRequestError.IMPORT_TOO_FEW_WAYPOINTS, RouteFileFailure.TooFewWaypoints.toRouteRequestError())
    }

    @Test
    fun `a coordinate out of range maps to the out-of-range import error`() {
        assertEquals(
            RouteRequestError.IMPORT_COORDINATE_OUT_OF_RANGE,
            RouteFileFailure.CoordinateOutOfRange(RoutePoint(200.0, 0.0)).toRouteRequestError(),
        )
    }

    @Test
    fun `a malformed file maps to the malformed import error`() {
        assertEquals(RouteRequestError.IMPORT_MALFORMED, RouteFileFailure.Malformed.toRouteRequestError())
    }
}
