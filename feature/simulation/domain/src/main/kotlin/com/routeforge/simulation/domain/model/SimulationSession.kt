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
    val speedSetting: SpeedSetting = SpeedSetting.DEFAULT,
    val executionMode: ExecutionMode = ExecutionMode.DEFAULT,
    val completedRuns: Int = 0,
    /** True while traveling [route]'s geometry back toward its start — only ever set when
     *  [executionMode] is [ExecutionMode.PingPong]; irrelevant otherwise. */
    val isReversed: Boolean = false,
)
