package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.NamedPlace
import kotlinx.coroutines.flow.Flow

/**
 * Hand-off of a favorite picked for the route, from the Favorites screen (picker mode) to route creation, which appends it
 * to the current draft as a new waypoint without any further confirmation — selecting an existing favorite is already an
 * explicit, confirming action.
 *
 * Carries a [NamedPlace], so the favorite's name reaches route creation. Sender: `FavoritesViewModel`. Receiver: `RouteDraftEditor`. Delivered once.
 */
interface SelectedFavoriteWaypointHandoffSender {
    fun send(place: NamedPlace)
}

interface SelectedFavoriteWaypointHandoffReceiver {
    val incoming: Flow<NamedPlace>
}

class SelectedFavoriteWaypointHandoffImpl :
    SelectedFavoriteWaypointHandoffSender,
    SelectedFavoriteWaypointHandoffReceiver {
    private val slot = OneShotSlot<NamedPlace>()

    /** What has been sent and not yet taken; for tests, it is on neither side. */
    val pending: NamedPlace? get() = slot.pending

    override val incoming: Flow<NamedPlace> = slot.incoming

    override fun send(place: NamedPlace) = slot.send(place)
}
