package com.routeforge.coredomain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private fun testRegion(
    tileCount: Int,
    approximateSizeBytes: Long,
    missingTileCount: Int,
) = Region(
    id = "region",
    displayName = "Region",
    minLatitude = 0.0,
    minLongitude = 0.0,
    maxLatitude = 1.0,
    maxLongitude = 1.0,
    tileIds = List(tileCount) { "tile$it" },
    approximateSizeBytes = approximateSizeBytes,
    status = RegionStatus.NOT_DOWNLOADED,
    missingTileCount = missingTileCount,
)

class RegionTest {
    @Test
    fun `estimatedMissingBytes multiplies before dividing, so the per-tile remainder isn't lost`() {
        // 7 bytes / 3 tiles truncates to 2 if divided first, losing the remainder across many tiles.
        val region = testRegion(tileCount = 3, approximateSizeBytes = 7L, missingTileCount = 3)

        assertEquals(7L, region.estimatedMissingBytes)
    }

    @Test
    fun `estimatedMissingBytes is zero when nothing is missing`() {
        val region = testRegion(tileCount = 3, approximateSizeBytes = 9L, missingTileCount = 0)

        assertEquals(0L, region.estimatedMissingBytes)
    }

    @Test
    fun `estimatedMissingBytes is zero when there are no tiles`() {
        val region = testRegion(tileCount = 0, approximateSizeBytes = 9L, missingTileCount = 0)

        assertEquals(0L, region.estimatedMissingBytes)
    }
}
