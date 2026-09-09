package com.routeforge.routing.data

import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import com.routeforge.routing.domain.model.RoutePoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private val coveredRegion =
    Region(
        id = "covered",
        displayName = "Covered",
        minLatitude = -1.0,
        minLongitude = -1.0,
        maxLatitude = 1.0,
        maxLongitude = 1.0,
        tileIds = listOf("E0_N0.rd5"),
        approximateSizeBytes = 1L,
        status = RegionStatus.BUNDLED,
    )

class BrouterRoutingEngineTest {
    @Test
    fun `computePath returns null when no tile data covers the requested points`(
        @TempDir emptySegmentDir: File,
    ) {
        val engine = BrouterRoutingEngine(segmentDir = emptySegmentDir, profileFile = testProfileFile(emptySegmentDir))

        val result =
            engine.computePath(
                listOf(
                    RoutePoint(latitude = 0.1, longitude = 0.1),
                    RoutePoint(latitude = 0.2, longitude = 0.2),
                ),
            )

        assertNull(result)
    }

    @Test
    fun `computePath returns null when fewer than two points are supplied`(
        @TempDir emptySegmentDir: File,
    ) {
        val engine = BrouterRoutingEngine(segmentDir = emptySegmentDir, profileFile = testProfileFile(emptySegmentDir))

        val result = engine.computePath(listOf(RoutePoint(latitude = 0.1, longitude = 0.1)))

        assertNull(result)
    }

    @Test
    fun `snap leaves a point unsnapped when its region has no matching tile data on disk`(
        @TempDir emptySegmentDir: File,
    ) {
        val engine = BrouterRoutingEngine(segmentDir = emptySegmentDir, profileFile = testProfileFile(emptySegmentDir))
        val point = RoutePoint(latitude = 0.1, longitude = 0.1)

        val result = engine.snap(point, availableRegions = listOf(coveredRegion))

        assertNull(result.snappedLatitude)
        assertNull(result.snappedLongitude)
    }

    @Test
    fun `snap returns the point unchanged without touching the engine when outside every known region`(
        @TempDir emptySegmentDir: File,
    ) {
        val engine = BrouterRoutingEngine(segmentDir = emptySegmentDir, profileFile = testProfileFile(emptySegmentDir))
        val point = RoutePoint(latitude = 50.0, longitude = 50.0)

        val result = engine.snap(point, availableRegions = listOf(coveredRegion))

        assertEquals(point, result)
    }
}

private fun testProfileFile(segmentDir: File): File {
    val profileDir = File(segmentDir, "profile").apply { mkdirs() }
    copyResourceTo("profile/car-fast.brf", File(profileDir, "car-fast.brf"))
    copyResourceTo("profile/lookups.dat", File(profileDir, "lookups.dat"))
    return File(profileDir, "car-fast.brf")
}

private fun copyResourceTo(
    resourcePath: String,
    target: File,
) {
    val resourceStream = requireNotNull(BrouterRoutingEngineTest::class.java.classLoader?.getResourceAsStream(resourcePath))
    resourceStream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
}
