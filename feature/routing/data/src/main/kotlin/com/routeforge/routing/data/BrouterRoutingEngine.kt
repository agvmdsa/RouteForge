package com.routeforge.routing.data

import btools.mapaccess.OsmNode
import btools.router.OsmNodeNamed
import btools.router.OsmTrack
import btools.router.RoutingContext
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.RoutingEngine
import com.routeforge.routing.domain.model.Region
import java.io.File
import btools.router.RoutingEngine as BrouterEngine

class BrouterRoutingEngine(
    private val segmentDir: File,
    private val profileFile: File,
) : RoutingEngine {
    override fun snap(
        point: RoutePoint,
        availableRegions: List<Region>,
    ): RoutePoint {
        val isWithinKnownCoverage = availableRegions.any { it.contains(point.latitude, point.longitude) }
        if (!isWithinKnownCoverage) return point

        val duplicatedWaypoint =
            listOf(
                toOsmNodeNamed(point, "snap_start"),
                toOsmNodeNamed(point, "snap_end"),
            )
        val track = runEngine(duplicatedWaypoint) ?: return point
        val matchedNode = track.nodes.firstOrNull() ?: return point
        return point.copy(
            snappedLatitude = decodeLatitude(matchedNode.getILat()),
            snappedLongitude = decodeLongitude(matchedNode.getILon()),
        )
    }

    override fun computePath(points: List<RoutePoint>): Route? {
        if (points.size < 2) return null

        val waypoints = points.mapIndexed { index, point -> toOsmNodeNamed(point, "wp$index") }
        val track = runEngine(waypoints) ?: return null

        val geometry = track.nodes.map { decodeLatitude(it.getILat()) to decodeLongitude(it.getILon()) }
        return Route(
            points = points,
            geometry = geometry,
            distanceMeters = track.distance.toDouble(),
        )
    }

    private fun runEngine(waypoints: List<OsmNodeNamed>): OsmTrack? {
        val routingContext =
            RoutingContext().apply {
                localFunction = profileFile.absolutePath
            }
        val engine = BrouterEngine(null, null, segmentDir, ArrayList(waypoints), routingContext)
        engine.run()
        return if (engine.getErrorMessage() != null) null else engine.getFoundTrack()
    }

    private fun toOsmNodeNamed(
        point: RoutePoint,
        waypointName: String,
    ): OsmNodeNamed {
        val latitude = point.snappedLatitude ?: point.latitude
        val longitude = point.snappedLongitude ?: point.longitude
        val ilon = Math.round((longitude + 180.0) * 1_000_000.0).toInt()
        val ilat = Math.round((latitude + 90.0) * 1_000_000.0).toInt()
        return OsmNodeNamed(OsmNode(ilon, ilat)).apply { name = waypointName }
    }

    private fun decodeLongitude(ilon: Int): Double = ilon / 1_000_000.0 - 180.0

    private fun decodeLatitude(ilat: Int): Double = ilat / 1_000_000.0 - 90.0
}
