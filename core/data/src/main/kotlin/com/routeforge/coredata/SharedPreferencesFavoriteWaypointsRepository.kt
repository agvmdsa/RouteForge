package com.routeforge.coredata

import android.content.Context
import com.routeforge.coredomain.FavoriteWaypointsRepository
import com.routeforge.coredomain.model.FavoriteWaypoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    private val context: Context,
) : FavoriteWaypointsRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _favorites = MutableStateFlow(readFavorites())

    override fun observeFavorites(): StateFlow<List<FavoriteWaypoint>> = _favorites

    override fun add(
        name: String,
        latitude: Double,
        longitude: Double,
    ): FavoriteWaypoint {
        val favorite = FavoriteWaypoint(id = UUID.randomUUID().toString(), name = name, latitude = latitude, longitude = longitude)
        writeFavorites(_favorites.value + favorite)
        return favorite
    }

    override fun update(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double,
    ) {
        writeFavorites(
            _favorites.value.map { favorite ->
                if (favorite.id == id) favorite.copy(name = name, latitude = latitude, longitude = longitude) else favorite
            },
        )
    }

    override fun delete(id: String) {
        writeFavorites(_favorites.value.filterNot { it.id == id })
    }

    private fun writeFavorites(favorites: List<FavoriteWaypoint>) {
        _favorites.value = favorites
        val records = favorites.map { FavoriteWaypointRecord(it.id, it.name, it.latitude, it.longitude) }
        prefs.edit().putString(KEY_FAVORITES_JSON, json.encodeToString(records)).apply()
    }

    private fun readFavorites(): List<FavoriteWaypoint> {
        val text = prefs.getString(KEY_FAVORITES_JSON, null) ?: return emptyList()
        val records = json.decodeFromString<List<FavoriteWaypointRecord>>(text)
        return records.map { FavoriteWaypoint(it.id, it.name, it.latitude, it.longitude) }
    }
}
