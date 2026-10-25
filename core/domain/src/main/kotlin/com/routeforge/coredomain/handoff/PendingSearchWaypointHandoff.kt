package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.PlaceSearchResult
import kotlinx.coroutines.flow.Flow

/**
 * Hand-off of a place chosen while planning a route, from address search (opened while Plan Route was active) and from
 * the app (a place shared from another app while on the Plan tab) to route creation, which turns it into a pending
 * add-waypoint confirmation (the same one already used for map taps) rather than adding it silently — unlike a favorite
 * picked for the route (`SelectedFavoriteWaypointHandoff`), picking a search result is not already a confirming
 * action on its own.
 *
 * Carries a [PlaceSearchResult]. Senders: `AddressSearchViewModel` and `MainActivity`. Receiver:
 * `PendingWaypointConfirmer`. Delivered once.
 */
interface PendingSearchWaypointHandoffSender {
    fun send(result: PlaceSearchResult)
}

interface PendingSearchWaypointHandoffReceiver {
    val incoming: Flow<PlaceSearchResult>
}

class PendingSearchWaypointHandoffImpl :
    PendingSearchWaypointHandoffSender,
    PendingSearchWaypointHandoffReceiver {
    private val slot = OneShotSlot<PlaceSearchResult>()

    /** What has been sent and not yet taken; for tests, it is on neither side. */
    val pending: PlaceSearchResult? get() = slot.pending

    override val incoming: Flow<PlaceSearchResult> = slot.incoming

    override fun send(result: PlaceSearchResult) = slot.send(result)
}
