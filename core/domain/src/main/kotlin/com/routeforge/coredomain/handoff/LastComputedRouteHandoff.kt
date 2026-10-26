package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hand-off of the route that is loaded for simulation: the one route creation or a saved route produced last, or the one
 * the mode switch on the Simulation screen swapped it for. It is cleared when the simulation stops, when a joystick session
 * takes over or when a teleport replaces the mocked session. Simulation reads it to show the loaded route; the storage rules
 * read it to know which map regions the route still needs. The latest value is kept and any number of readers observe it.
 *
 * Carries a [Route], `null` at the start. Writers: `RouteComputer`, `RouteModePreviewer`, `SavedRoutesViewModel`,
 * `RouteModeSwitcher`. Clearers (they only clear): `PlaybackControls`, `JoystickSessionController`,
 * `PendingTeleportConfirmer`. Readers: `SimulationViewModel`, `ComputeRegionUsageUseCase`, `EnforceStorageQuotaUseCase`.
 * Latest value kept.
 */
interface LastComputedRouteHandoffWriter {
    fun set(route: Route)
}

interface LastComputedRouteHandoffClearer {
    fun clear()
}

interface LastComputedRouteHandoffReader {
    val route: StateFlow<Route?>
}

class LastComputedRouteHandoffImpl :
    LastComputedRouteHandoffWriter,
    LastComputedRouteHandoffClearer,
    LastComputedRouteHandoffReader {
    private val lastComputed = MutableStateFlow<Route?>(null)

    override val route: StateFlow<Route?> = lastComputed.asStateFlow()

    override fun set(route: Route) {
        lastComputed.value = route
    }

    override fun clear() {
        lastComputed.value = null
    }
}
