package com.routeforge.coredomain.model

/** A route that arrived from another app and is waiting for Plan Route to take it. */
sealed interface IncomingRoute {
    /** The bytes of a `.gpx` file, read at once because the permission to read it lasts only as long as the request. */
    class GpxFile(
        val bytes: ByteArray,
    ) : IncomingRoute

    /** The stops of a directions link, in order. */
    data class Stops(
        val points: List<RoutePoint>,
    ) : IncomingRoute
}
