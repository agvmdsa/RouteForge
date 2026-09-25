package com.routeforge.routing.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BrouterTileGridTest {
    @Test
    fun `a point in the northeast quadrant maps to the matching bundled tile id`() {
        assertEquals("E10_N50", BrouterTileGrid.tileIdFor(latitude = 52.5, longitude = 13.4))
    }

    @Test
    fun `a point in the southwest quadrant floors toward more negative coordinates`() {
        assertEquals("W35_S10", BrouterTileGrid.tileIdFor(latitude = -8.05, longitude = -34.9))
    }

    @Test
    fun `a coordinate exactly on a grid line belongs to the cell starting at that line`() {
        assertEquals("E10_N50", BrouterTileGrid.tileIdFor(latitude = 50.0, longitude = 10.0))
    }

    @Test
    fun `bounds for a computed tile id round-trip back to a five by five degree cell`() {
        val bounds = BrouterTileGrid.boundsFor("W35_S10")

        assertEquals(TileBounds(minLatitude = -10.0, minLongitude = -35.0, maxLatitude = -5.0, maxLongitude = -30.0), bounds)
    }

    @Test
    fun `an id that does not match the grid pattern has no bounds`() {
        assertNull(BrouterTileGrid.boundsFor("not-a-tile"))
    }

    @Test
    fun `file name for a tile id appends the rd5 extension`() {
        assertEquals("E10_N50.rd5", BrouterTileGrid.fileNameFor("E10_N50"))
    }
}
