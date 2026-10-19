package com.routeforge.coredomain.holder

import com.routeforge.coredomain.model.IncomingRoute
import com.routeforge.coredomain.model.RoutePoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class PendingIncomingRouteHolderTest {
    @Test
    fun `starts with no route waiting`() {
        assertNull(PendingIncomingRouteHolder().route.value)
    }

    @Test
    fun `a file that was set is waiting until it is cleared`() {
        val holder = PendingIncomingRouteHolder()
        val file = IncomingRoute.GpxFile(byteArrayOf(1, 2, 3))

        holder.set(file)
        assertSame(file, holder.route.value)

        holder.clear()
        assertNull(holder.route.value)
    }

    @Test
    fun `stops that were set are waiting, and a newer route replaces an older one`() {
        val holder = PendingIncomingRouteHolder()
        val stops = IncomingRoute.Stops(listOf(RoutePoint(52.5, 13.4), RoutePoint(52.6, 13.5)))

        holder.set(IncomingRoute.GpxFile(byteArrayOf(9)))
        holder.set(stops)

        assertEquals(stops, holder.route.value)
    }
}
