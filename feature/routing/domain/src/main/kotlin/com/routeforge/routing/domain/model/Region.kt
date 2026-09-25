package com.routeforge.routing.domain.model

data class Region(
    val id: String,
    val displayName: String,
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double,
    val tileIds: List<String>,
    val approximateSizeBytes: Long,
    val status: RegionStatus,
    val missingTileCount: Int = 0,
) {
    fun contains(
        latitude: Double,
        longitude: Double,
    ): Boolean = latitude in minLatitude..maxLatitude && longitude in minLongitude..maxLongitude

    /** Estimated download size for only the missing tiles, assuming an even split of [approximateSizeBytes] across [tileIds]. */
    val estimatedMissingBytes: Long
        get() = if (tileIds.isEmpty() || missingTileCount <= 0) 0L else (approximateSizeBytes / tileIds.size) * missingTileCount
}
