package com.routeforge.coredomain.model

import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private val sampleRoute =
    Route(points = emptyList(), geometry = listOf(0.0 to 0.0, 1.0 to 1.0), distanceMeters = 100.0)

class RouteOptionsTest {
    @Test
    fun `autoResolved picks Free-roam when Guided is not viable`() {
        val options = RouteOptions(guided = null, freeRoam = sampleRoute)

        assertEquals(sampleRoute to RoutePlaybackMode.FREE_ROAM, options.autoResolved)
    }

    @Test
    fun `autoResolved is null when both modes are viable`() {
        val options = RouteOptions(guided = sampleRoute, freeRoam = sampleRoute)

        assertNull(options.autoResolved)
    }
}
