package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.RealLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hand-off of the last real location the device reported, so that simulation, route creation and address search can start
 * from where the user really is without a direct feature-to-feature dependency. The latest location is kept and any number
 * of readers observe it.
 *
 * Carries a [RealLocation], `null` until the first fix. Writer: `RealLocationTracker`. Readers: `RealLocationTracker`,
 * `SimulationControllerImpl`, `RouteRequestViewModel`, `SearchPlacesUseCase`. Latest value kept.
 */
interface LastKnownRealLocationHandoffWriter {
    fun set(location: RealLocation)
}

interface LastKnownRealLocationHandoffReader {
    val location: StateFlow<RealLocation?>
}

class LastKnownRealLocationHandoffImpl :
    LastKnownRealLocationHandoffWriter,
    LastKnownRealLocationHandoffReader {
    private val lastKnown = MutableStateFlow<RealLocation?>(null)

    override val location: StateFlow<RealLocation?> = lastKnown.asStateFlow()

    override fun set(location: RealLocation) {
        lastKnown.value = location
    }
}
