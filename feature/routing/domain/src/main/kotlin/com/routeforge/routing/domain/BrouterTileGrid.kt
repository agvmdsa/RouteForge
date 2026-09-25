package com.routeforge.routing.domain

import kotlin.math.floor

private const val GRID_STEP_DEGREES = 5

data class TileBounds(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double,
)

/**
 * BRouter's public segment server (brouter.de/brouter/segments4/) partitions the entire planet
 * into a fixed 5°x5° grid, one `.rd5` file per cell, named after the cell's south-west corner
 * (e.g. `E10_N50.rd5` for the cell spanning 10-15°E, 50-55°N). Since the naming is fully
 * determined by coordinates, any point's tile — and therefore the exact file that needs
 * downloading — can be computed directly, with no manually curated list of "known regions"
 * required for global coverage.
 */
object BrouterTileGrid {
    private val TILE_ID_REGEX = Regex("""^([EW])(\d+)_([NS])(\d+)$""")

    /** The id (without the `.rd5` extension) of the grid cell containing [latitude]/[longitude]. */
    fun tileIdFor(
        latitude: Double,
        longitude: Double,
    ): String {
        val minLongitude = floorToStep(longitude)
        val minLatitude = floorToStep(latitude)
        val lonPart = if (minLongitude < 0) "W${-minLongitude}" else "E$minLongitude"
        val latPart = if (minLatitude < 0) "S${-minLatitude}" else "N$minLatitude"
        return "${lonPart}_$latPart"
    }

    /** Parses a tile id (e.g. "W35_S10") back into its cell bounds, or null if it isn't a valid grid id. */
    fun boundsFor(tileId: String): TileBounds? {
        val match = TILE_ID_REGEX.matchEntire(tileId) ?: return null
        val (lonSign, lonMagnitude, latSign, latMagnitude) = match.destructured
        val minLongitude = if (lonSign == "W") -lonMagnitude.toDouble() else lonMagnitude.toDouble()
        val minLatitude = if (latSign == "S") -latMagnitude.toDouble() else latMagnitude.toDouble()
        return TileBounds(
            minLatitude = minLatitude,
            minLongitude = minLongitude,
            maxLatitude = minLatitude + GRID_STEP_DEGREES,
            maxLongitude = minLongitude + GRID_STEP_DEGREES,
        )
    }

    fun fileNameFor(tileId: String): String = "$tileId.rd5"

    private fun floorToStep(value: Double): Int = (floor(value / GRID_STEP_DEGREES) * GRID_STEP_DEGREES).toInt()
}
