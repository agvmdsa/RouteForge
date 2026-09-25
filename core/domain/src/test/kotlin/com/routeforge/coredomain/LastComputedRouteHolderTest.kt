package com.routeforge.coredomain

import com.routeforge.coredomain.model.Route
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LastComputedRouteHolderTest {
    private val route = Route(points = emptyList(), geometry = emptyList(), distanceMeters = 0.0)

    @Test
    fun `starts empty`() {
        assertNull(LastComputedRouteHolder().route.value)
    }

    @Test
    fun `set stores the route`() {
        val holder = LastComputedRouteHolder()

        holder.set(route)

        assertEquals(route, holder.route.value)
    }

    @Test
    fun `clear removes a previously set route`() {
        val holder = LastComputedRouteHolder()
        holder.set(route)

        holder.clear()

        assertNull(holder.route.value)
    }
}
