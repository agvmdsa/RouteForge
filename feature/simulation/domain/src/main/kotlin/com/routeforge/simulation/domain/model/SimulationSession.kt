package com.routeforge.simulation.domain.model

import com.routeforge.coredomain.model.Route

data class SimulationSession(
    val mode: SimulationMode,
    val status: SimulationStatus,
    val latitude: Double,
    val longitude: Double,
    val bearingDegrees: Float,
    val speedMetersPerSecond: Float,
    val route: Route?,
    val distanceTraveledMeters: Double,
)
