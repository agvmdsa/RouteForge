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
) {
    fun contains(
        latitude: Double,
        longitude: Double,
    ): Boolean = latitude in minLatitude..maxLatitude && longitude in minLongitude..maxLongitude
}
