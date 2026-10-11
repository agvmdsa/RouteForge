package com.routeforge.coredomain.holder

import com.routeforge.coredomain.model.PlaceSearchResult
import kotlinx.coroutines.flow.StateFlow

/** Hand-off from address search (opened while Plan Route was active) to route creation, which
 *  turns this into a pending add-waypoint confirmation (the same one already used for map taps)
 *  rather than adding it silently — unlike [SelectedFavoriteWaypointHolder], picking a search
 *  result is not already a confirming action on its own. */
class PendingSearchWaypointHolder {
    private val holder = HandoffHolder<PlaceSearchResult?>(initial = null)
    val selected: StateFlow<PlaceSearchResult?> = holder.value

    fun set(result: PlaceSearchResult) = holder.set(result)

    fun clear() = holder.set(null)
}
