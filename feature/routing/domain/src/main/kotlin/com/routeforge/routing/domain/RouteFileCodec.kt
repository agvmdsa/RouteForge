package com.routeforge.routing.domain

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFailure

/**
 * FR-011–FR-015: parses/serializes a route to/from a specific
 * [com.routeforge.routing.domain.model.RouteFileFormat]. Implementations live in
 * `feature/routing/data` (file I/O is a data-layer concern).
 */
interface RouteFileCodec {
    fun parse(bytes: ByteArray): Result<RouteDraft, RouteFileFailure>

    fun serialize(route: Route): ByteArray
}
