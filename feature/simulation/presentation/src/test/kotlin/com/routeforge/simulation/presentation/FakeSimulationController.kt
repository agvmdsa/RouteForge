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

    private val _mockedSession = MutableStateFlow<SimulationSession?>(null)
    override val mockedSession: StateFlow<SimulationSession?> = _mockedSession

    val teleportCalls = mutableListOf<TeleportCall>()
    val startRouteCalls = mutableListOf<StartRouteCall>()
    var pauseCallCount = 0
        private set
    var resumeCallCount = 0
        private set
    var stopCallCount = 0
        private set

    fun emit(mockedSession: SimulationSession?) {
        _mockedSession.value = mockedSession
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
