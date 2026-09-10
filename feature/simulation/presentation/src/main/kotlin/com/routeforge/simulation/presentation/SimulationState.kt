package com.routeforge.simulation.presentation

import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.model.SimulationSession

data class SimulationState(
    val session: SimulationSession? = null,
    val pendingTeleportLatitude: Double? = null,
    val pendingTeleportLongitude: Double? = null,
    val loadedRoute: Route? = null,
    val speedInput: String = "",
    val errorMessage: String? = null,
    val isBlockedByAuthorization: Boolean = false,
)
