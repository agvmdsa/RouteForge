package com.routeforge.coredomain.holder

import com.routeforge.coredomain.model.PlaceSearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Hand-off from address search (opened while Plan Route was active) to route creation, which
 *  turns this into a pending add-waypoint confirmation (the same one already used for map taps)
 *  rather than adding it silently — unlike [SelectedFavoriteWaypointHolder], picking a search
 *  result is not already a confirming action on its own. */
class PendingSearchWaypointHolder {
    private val _selected = MutableStateFlow<PlaceSearchResult?>(null)
    val selected: StateFlow<PlaceSearchResult?> = _selected.asStateFlow()

    fun set(result: PlaceSearchResult) {
        _selected.value = result
    }

    fun clear() {
        _selected.value = null
    }
}
