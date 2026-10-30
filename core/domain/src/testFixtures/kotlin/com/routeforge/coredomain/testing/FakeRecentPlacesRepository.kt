package com.routeforge.coredomain.testing

import com.routeforge.coredomain.RecentPlacesRepository
import com.routeforge.coredomain.model.NamedPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Keeps every [record] call in [recorded] and does not apply the dedupe rules of `RecentPlaces`, which have their own test. */
class FakeRecentPlacesRepository(
    initialRecents: List<NamedPlace> = emptyList(),
) : RecentPlacesRepository {
    private val recentsState = MutableStateFlow(initialRecents)

    val recorded = mutableListOf<NamedPlace>()

    override fun observeRecents(): StateFlow<List<NamedPlace>> = recentsState

    override fun record(place: NamedPlace) {
        recorded.add(place)
        recentsState.value = listOf(place) + recentsState.value
    }

    override fun remove(place: NamedPlace) {
        recentsState.value = recentsState.value.filterNot { it.latitude == place.latitude && it.longitude == place.longitude }
    }

    override fun clear() {
        recentsState.value = emptyList()
    }
}
