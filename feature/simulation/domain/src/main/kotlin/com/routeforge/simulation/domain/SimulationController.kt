package com.routeforge.simulation.domain

import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.model.SimulationSession
import kotlinx.coroutines.flow.StateFlow

interface SimulationController {
    val session: StateFlow<SimulationSession?>

    fun teleport(
        latitude: Double,
        longitude: Double,
    )

    fun startRoute(
        route: Route,
        speedMetersPerSecond: Float,
    )

    fun pause()

    fun resume()

    fun stop()
}
