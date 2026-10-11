package com.routeforge.coredata

import android.content.Context
import com.routeforge.coredomain.FavoriteRoutesRepository
import com.routeforge.coredomain.model.FavoriteRoute
import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
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

private fun FavoriteRoute.toRecord(): FavoriteRouteRecord =
    FavoriteRouteRecord(
        id = id,
        name = name,
        points = points.map { point -> RoutePointRecord(point.latitude, point.longitude, point.snappedLatitude, point.snappedLongitude) },
    )

private fun FavoriteRouteRecord.toDomain(): FavoriteRoute =
    FavoriteRoute(
        id = id,
        name = name,
        points = points.map { RoutePoint(it.latitude, it.longitude, it.snappedLatitude, it.snappedLongitude) },
    )

class SharedPreferencesFavoriteRoutesRepository(
    context: Context,
) : FavoriteRoutesRepository {
    private val store =
        JsonPrefsListStore(
            context = context,
            prefsName = PREFS_NAME,
            key = KEY_FAVORITE_ROUTES_JSON,
            recordListSerializer = ListSerializer(FavoriteRouteRecord.serializer()),
            toRecord = FavoriteRoute::toRecord,
            toDomain = FavoriteRouteRecord::toDomain,
            tag = "FavoriteRoutesRepo",
        )

    override fun observeFavoriteRoutes(): StateFlow<List<FavoriteRoute>> = store.items

    override fun add(
        name: String,
        points: List<RoutePoint>,
    ): FavoriteRoute {
        val favoriteRoute = FavoriteRoute(id = UUID.randomUUID().toString(), name = name, points = points)
        store.write(store.items.value + favoriteRoute)
        return favoriteRoute
    }

    override fun rename(
        id: String,
        name: String,
    ) {
        store.write(
            store.items.value.map { favoriteRoute ->
                if (favoriteRoute.id == id) favoriteRoute.copy(name = name) else favoriteRoute
            },
        )
    }

    override fun delete(id: String) {
        store.write(store.items.value.filterNot { it.id == id })
    }
}
