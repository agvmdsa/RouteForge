package com.routeforge.simulation.presentation

import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.SimulationController
import com.routeforge.simulation.domain.model.SimulationSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSimulationController : SimulationController {
    data class TeleportCall(
        val latitude: Double,
        val longitude: Double,
    )

    data class StartRouteCall(
        val route: Route,
        val speedMetersPerSecond: Float,
    )

    private val _session = MutableStateFlow<SimulationSession?>(null)
    override val session: StateFlow<SimulationSession?> = _session

    val teleportCalls = mutableListOf<TeleportCall>()
    val startRouteCalls = mutableListOf<StartRouteCall>()
    var pauseCallCount = 0
        private set
    var resumeCallCount = 0
        private set
    var stopCallCount = 0
        private set

    fun emit(session: SimulationSession?) {
        _session.value = session
    }

    override fun teleport(
        latitude: Double,
        longitude: Double,
    ) {
        teleportCalls.add(TeleportCall(latitude, longitude))
    }

    override fun startRoute(
        route: Route,
        speedMetersPerSecond: Float,
    ) {
        startRouteCalls.add(StartRouteCall(route, speedMetersPerSecond))
    }

    override fun pause() {
        pauseCallCount++
    }

    override fun resume() {
        resumeCallCount++
    }

    override fun stop() {
        stopCallCount++
    }
}
