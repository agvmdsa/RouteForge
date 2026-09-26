package com.routeforge.simulation.domain

import com.routeforge.coredomain.model.RealLocation

/** A single item from [RealLocationDataSource.observeLocation] — either an actual position fix,
 *  or a terminal signal that no fix is coming (so callers can show a real error instead of an
 *  unbounded "still searching" state). */
sealed interface RealLocationUpdate {
    data class Fix(
        val location: RealLocation,
    ) : RealLocationUpdate

    data class Unavailable(
        val reason: RealLocationFailure,
    ) : RealLocationUpdate
}
