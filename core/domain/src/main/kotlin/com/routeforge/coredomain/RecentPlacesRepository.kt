package com.routeforge.coredomain

import com.routeforge.coredomain.model.NamedPlace
import kotlinx.coroutines.flow.StateFlow

interface RecentPlacesRepository {
    /** Newest used first. */
    fun observeRecents(): StateFlow<List<NamedPlace>>

    /** Applies [RecentPlaces.record] to the stored list and persists the result. */
    fun record(place: NamedPlace)

    /** Removes the entry with exactly these coordinates. */
    fun remove(place: NamedPlace)

    fun clear()
}
