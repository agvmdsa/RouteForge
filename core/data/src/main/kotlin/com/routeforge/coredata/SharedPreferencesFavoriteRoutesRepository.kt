package com.routeforge.coredata

import android.content.Context
import com.routeforge.coredomain.FavoriteRoutesRepository
import com.routeforge.coredomain.model.FavoriteRoute
import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private const val PREFS_NAME = "routeforge_favorite_routes"
private const val KEY_FAVORITE_ROUTES_JSON = "favorite_routes_json"

@Serializable
private data class RoutePointRecord(
    val latitude: Double,
    val longitude: Double,
    val snappedLatitude: Double? = null,
    val snappedLongitude: Double? = null,
)

@Serializable
private data class FavoriteRouteRecord(
    val id: String,
    val name: String,
    val points: List<RoutePointRecord>,
)

class SharedPreferencesFavoriteRoutesRepository(
    private val context: Context,
) : FavoriteRoutesRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _favoriteRoutes = MutableStateFlow(readFavoriteRoutes())

    override fun observeFavoriteRoutes(): StateFlow<List<FavoriteRoute>> = _favoriteRoutes

    override fun add(
        name: String,
        points: List<RoutePoint>,
    ): FavoriteRoute {
        val favoriteRoute = FavoriteRoute(id = UUID.randomUUID().toString(), name = name, points = points)
        writeFavoriteRoutes(_favoriteRoutes.value + favoriteRoute)
        return favoriteRoute
    }

    override fun rename(
        id: String,
        name: String,
    ) {
        writeFavoriteRoutes(
            _favoriteRoutes.value.map { favoriteRoute ->
                if (favoriteRoute.id == id) favoriteRoute.copy(name = name) else favoriteRoute
            },
        )
    }

    override fun delete(id: String) {
        writeFavoriteRoutes(_favoriteRoutes.value.filterNot { it.id == id })
    }

    private fun writeFavoriteRoutes(favoriteRoutes: List<FavoriteRoute>) {
        _favoriteRoutes.value = favoriteRoutes
        val records = favoriteRoutes.map { it.toRecord() }
        prefs.edit().putString(KEY_FAVORITE_ROUTES_JSON, json.encodeToString(records)).apply()
    }

    private fun readFavoriteRoutes(): List<FavoriteRoute> {
        val text = prefs.getString(KEY_FAVORITE_ROUTES_JSON, null) ?: return emptyList()
        val records = json.decodeFromString<List<FavoriteRouteRecord>>(text)
        return records.map { it.toDomain() }
    }

    private fun FavoriteRoute.toRecord(): FavoriteRouteRecord =
        FavoriteRouteRecord(
            id = id,
            name = name,
            points =
                points.map { point ->
                    RoutePointRecord(point.latitude, point.longitude, point.snappedLatitude, point.snappedLongitude)
                },
        )

    private fun FavoriteRouteRecord.toDomain(): FavoriteRoute =
        FavoriteRoute(
            id = id,
            name = name,
            points = points.map { RoutePoint(it.latitude, it.longitude, it.snappedLatitude, it.snappedLongitude) },
        )
}
