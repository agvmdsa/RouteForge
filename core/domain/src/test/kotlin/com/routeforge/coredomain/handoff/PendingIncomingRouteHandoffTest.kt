package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.IncomingRoute
import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PendingIncomingRouteHandoffTest {
    private val home: IncomingRoute = IncomingRoute.Stops(listOf(RoutePoint(52.5, 13.4), RoutePoint(52.6, 13.5)))
    private val work: IncomingRoute = IncomingRoute.GpxFile(byteArrayOf(9))

    @Test
    fun `nothing is pending before anything is sent`() {
        assertNull(PendingIncomingRouteHandoffImpl().pending)
    }

    @Test
    fun `a receiver that is already collecting gets the value that is sent`() =
        runTest {
            val handoff = PendingIncomingRouteHandoffImpl()
            val received = async(UnconfinedTestDispatcher(testScheduler)) { handoff.incoming.first() }

            handoff.send(home)

            assertEquals(home, received.await())
        }

    @Test
    fun `a value sent before anyone collects waits and is delivered when collection starts`() =
        runTest {
            val handoff = PendingIncomingRouteHandoffImpl()

            handoff.send(home)

            assertEquals(home, handoff.pending)
            assertEquals(home, handoff.incoming.first())
        }

    @Test
    fun `when two values are sent before one is taken the latest one is delivered`() =
        runTest {
            val handoff = PendingIncomingRouteHandoffImpl()

            handoff.send(home)
            handoff.send(work)

            assertEquals(work, handoff.incoming.first())
            assertNull(handoff.pending)
        }

    @Test
    fun `an equal value sent twice before it is taken is delivered once`() =
        runTest {
            val handoff = PendingIncomingRouteHandoffImpl()

            handoff.send(home)
            handoff.send(home)

            assertEquals(home, handoff.incoming.first())
            assertNull(withTimeoutOrNull(100) { handoff.incoming.first() })
        }

    @Test
    fun `a taken value is not delivered to a second collection and nothing is pending`() =
        runTest {
            val handoff = PendingIncomingRouteHandoffImpl()
            handoff.send(home)

            handoff.incoming.first()

            assertNull(handoff.pending)
            assertNull(withTimeoutOrNull(100) { handoff.incoming.first() })
        }

    @Test
    fun `an equal value sent again after a take is delivered again`() =
        runTest {
            val handoff = PendingIncomingRouteHandoffImpl()
            handoff.send(home)
            handoff.incoming.first()

            handoff.send(home)

            assertEquals(home, handoff.incoming.first())
        }
}
