package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.NamedPlace
import kotlinx.coroutines.flow.Flow

/**
 * Hand-off of a place to teleport to, from where the user confirmed it to the Simulation screen, which teleports at once
 * on a new value without asking again — the confirmation already happened where the value was sent (with a route
 * loaded, the Simulation screen first asks whether to cancel the route).
 *
 * Carries a [NamedPlace], so the name the place came with reaches the Simulation screen. Senders: `FavoritesViewModel` (after "mock here" is confirmed) and `AddressSearchViewModel`
 * (after teleport is chosen). Receiver: `PendingTeleportConfirmer`. Delivered once.
 */
interface PendingTeleportTargetHandoffSender {
    fun send(place: NamedPlace)
}

interface PendingTeleportTargetHandoffReceiver {
    val incoming: Flow<NamedPlace>
}

class PendingTeleportTargetHandoffImpl :
    PendingTeleportTargetHandoffSender,
    PendingTeleportTargetHandoffReceiver {
    private val slot = OneShotSlot<NamedPlace>()

    /** What has been sent and not yet taken; for tests, it is on neither side. */
    val pending: NamedPlace? get() = slot.pending

    override val incoming: Flow<NamedPlace> = slot.incoming

    override fun send(place: NamedPlace) = slot.send(place)
}
