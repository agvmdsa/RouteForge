package com.routeforge.coredata

import android.content.Context
import com.routeforge.coredomain.FavoriteWaypointsRepository
import com.routeforge.coredomain.model.FavoriteWaypoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.util.UUID

private const val PREFS_NAME = "routeforge_favorite_waypoints"
private const val KEY_FAVORITES_JSON = "favorites_json"

@Serializable
private data class FavoriteWaypointRecord(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

class SharedPreferencesFavoriteWaypointsRepository(
    context: Context,
) : FavoriteWaypointsRepository {
    private val store =
        JsonPrefsListStore(
            context = context,
            prefsName = PREFS_NAME,
            key = KEY_FAVORITES_JSON,
            recordListSerializer = ListSerializer(FavoriteWaypointRecord.serializer()),
            toRecord = { FavoriteWaypointRecord(it.id, it.name, it.latitude, it.longitude) },
            toDomain = { FavoriteWaypoint(it.id, it.name, it.latitude, it.longitude) },
            tag = "FavoriteWaypointsRepo",
        )

    override fun observeFavorites(): StateFlow<List<FavoriteWaypoint>> = store.items

    override fun add(
        name: String,
        latitude: Double,
        longitude: Double,
    ): FavoriteWaypoint {
        val favorite = FavoriteWaypoint(id = UUID.randomUUID().toString(), name = name, latitude = latitude, longitude = longitude)
        store.write(store.items.value + favorite)
        return favorite
    }

    override fun update(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double,
    ) {
        store.write(
            store.items.value.map { favorite ->
                if (favorite.id == id) favorite.copy(name = name, latitude = latitude, longitude = longitude) else favorite
            },
        )
    }

    override fun delete(id: String) {
        store.write(store.items.value.filterNot { it.id == id })
    }
}
