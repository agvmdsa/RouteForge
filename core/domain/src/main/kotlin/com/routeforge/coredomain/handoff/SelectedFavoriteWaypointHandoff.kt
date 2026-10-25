package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.Flow

/**
 * Hand-off of a favorite picked for the route, from the Favorites screen (picker mode) to route creation, which appends it
 * to the current draft as a new waypoint without any further confirmation — selecting an existing favorite is already an
 * explicit, confirming action.
 *
 * Carries a [RoutePoint]. Sender: `FavoritesViewModel`. Receiver: `RouteDraftEditor`. Delivered once.
 */
interface SelectedFavoriteWaypointHandoffSender {
    fun send(point: RoutePoint)
}

interface SelectedFavoriteWaypointHandoffReceiver {
    val incoming: Flow<RoutePoint>
}

class SelectedFavoriteWaypointHandoffImpl :
    SelectedFavoriteWaypointHandoffSender,
    SelectedFavoriteWaypointHandoffReceiver {
    private val slot = OneShotSlot<RoutePoint>()

    /** What has been sent and not yet taken; for tests, it is on neither side. */
    val pending: RoutePoint? get() = slot.pending

    override val incoming: Flow<RoutePoint> = slot.incoming

    override fun send(point: RoutePoint) = slot.send(point)
}
