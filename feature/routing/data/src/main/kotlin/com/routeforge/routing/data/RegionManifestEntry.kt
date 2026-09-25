package com.routeforge.routing.data

import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import kotlinx.serialization.Serializable

@Serializable
data class RegionManifestEntry(
    val id: String,
    val displayName: String,
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double,
    val tileIds: List<String>,
    val approximateSizeBytes: Long,
) {
    fun toRegion(
        resolvedStatus: RegionStatus,
        missingTileCount: Int,
    ): Region =
        Region(
            id = id,
            displayName = displayName,
            minLatitude = minLatitude,
            minLongitude = minLongitude,
            maxLatitude = maxLatitude,
            maxLongitude = maxLongitude,
            tileIds = tileIds,
            approximateSizeBytes = approximateSizeBytes,
            status = resolvedStatus,
            missingTileCount = missingTileCount,
        )
}
