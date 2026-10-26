package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.IncomingRoute
import kotlinx.coroutines.flow.Flow

/**
 * Hand-off of a route another app handed over (a `.gpx` file or a directions link), from the app to route creation, which
 * turns it into the draft, asking first when the draft already has waypoints. It is applied once: taking the value is what
 * removes it.
 *
 * Carries an [IncomingRoute]. Sender: `MainActivity`. Receiver: `IncomingRouteReceiver`. Delivered once.
 */
interface PendingIncomingRouteHandoffSender {
    fun send(route: IncomingRoute)
}

interface PendingIncomingRouteHandoffReceiver {
    val incoming: Flow<IncomingRoute>
}

class PendingIncomingRouteHandoffImpl :
    PendingIncomingRouteHandoffSender,
    PendingIncomingRouteHandoffReceiver {
    private val slot = OneShotSlot<IncomingRoute>()

    /** What has been sent and not yet taken; for tests, it is on neither side. */
    val pending: IncomingRoute? get() = slot.pending

    override val incoming: Flow<IncomingRoute> = slot.incoming

    override fun send(route: IncomingRoute) = slot.send(route)
}
