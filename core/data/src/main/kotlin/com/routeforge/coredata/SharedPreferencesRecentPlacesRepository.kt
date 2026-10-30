package com.routeforge.coredata

import android.content.Context
import com.routeforge.coredomain.RecentPlaces
import com.routeforge.coredomain.RecentPlacesRepository
import com.routeforge.coredomain.model.NamedPlace
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

private const val PREFS_NAME = "routeforge_recent_places"
private const val KEY_RECENT_PLACES_JSON = "recent_places_json"

@Serializable
private data class RecentPlaceRecord(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
)

internal class SharedPreferencesRecentPlacesRepository(
    context: Context,
) : RecentPlacesRepository {
    private val store =
        JsonPrefsListStore(
            context = context,
            prefsName = PREFS_NAME,
            key = KEY_RECENT_PLACES_JSON,
            recordListSerializer = ListSerializer(RecentPlaceRecord.serializer()),
            toRecord = { RecentPlaceRecord(it.latitude, it.longitude, it.name) },
            toDomain = { NamedPlace(it.latitude, it.longitude, it.name) },
            tag = "RecentPlacesRepo",
        )

    override fun observeRecents(): StateFlow<List<NamedPlace>> = store.items

    override fun record(place: NamedPlace) {
        store.update { RecentPlaces.record(it, place) }
    }

    override fun remove(place: NamedPlace) {
        store.removeWhere { it.latitude == place.latitude && it.longitude == place.longitude }
    }

    override fun clear() {
        store.update { emptyList() }
    }
}
