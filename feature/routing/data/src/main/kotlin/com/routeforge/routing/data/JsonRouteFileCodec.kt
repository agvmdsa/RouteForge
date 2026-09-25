package com.routeforge.routing.data

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.RouteFileCodec
import com.routeforge.routing.domain.model.RouteDraft
import com.routeforge.routing.domain.model.RouteFileFailure
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val LATITUDE_RANGE = -90.0..90.0
private val LONGITUDE_RANGE = -180.0..180.0

@Serializable
private data class RouteFileJson(
    val name: String? = null,
    val id: String? = null,
    val waypoints: List<WaypointJson> = emptyList(),
)

@Serializable
private data class WaypointJson(
    val latitude: Double,
    val longitude: Double,
)

/** FR-011–FR-015: JSON route file format, per the documented `{name, id, waypoints[]}` schema. */
class JsonRouteFileCodec : RouteFileCodec {
    private val json = Json { ignoreUnknownKeys = true }

    override fun parse(bytes: ByteArray): Result<RouteDraft, RouteFileFailure> {
        val parsed =
            try {
                json.decodeFromString<RouteFileJson>(bytes.decodeToString())
            } catch (e: Exception) {
                return Result.Error(RouteFileFailure.Malformed)
            }

        if (parsed.waypoints.size < 2) {
            return Result.Error(RouteFileFailure.TooFewWaypoints)
        }

        val points = mutableListOf<RoutePoint>()
        for (waypoint in parsed.waypoints) {
            if (waypoint.latitude !in LATITUDE_RANGE || waypoint.longitude !in LONGITUDE_RANGE) {
                return Result.Error(
                    RouteFileFailure.CoordinateOutOfRange(
                        RoutePoint(latitude = waypoint.latitude, longitude = waypoint.longitude),
                    ),
                )
            }
            points.add(RoutePoint(latitude = waypoint.latitude, longitude = waypoint.longitude))
        }
        return Result.Success(RouteDraft(points = points))
    }

    override fun serialize(route: Route): ByteArray {
        val fileJson = RouteFileJson(waypoints = route.points.map { WaypointJson(it.latitude, it.longitude) })
        return json.encodeToString(fileJson).toByteArray(Charsets.UTF_8)
    }
}
