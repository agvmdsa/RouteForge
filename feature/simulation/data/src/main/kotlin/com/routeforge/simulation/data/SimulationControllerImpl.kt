package com.routeforge.simulation.data

import android.content.Context
import android.content.Intent
import com.routeforge.coredomain.MockLocationAuthorizationChecker
import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.MockLocationPublisher
import com.routeforge.simulation.domain.RouteProgressCalculator
import com.routeforge.simulation.domain.SimulationController
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SimulationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val DEFAULT_TICK_INTERVAL_MILLIS = 1_000L

class SimulationControllerImpl(
    private val mockLocationPublisher: MockLocationPublisher,
    private val mockLocationAuthorizationChecker: MockLocationAuthorizationChecker,
    private val context: Context? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val tickIntervalMillis: Long = DEFAULT_TICK_INTERVAL_MILLIS,
) : SimulationController {
    private val routeProgressCalculator = RouteProgressCalculator()

    private val _session = MutableStateFlow<SimulationSession?>(null)
    override val session: StateFlow<SimulationSession?> = _session.asStateFlow()

    private var tickJob: Job? = null

    override fun teleport(
        latitude: Double,
        longitude: Double,
    ) {
        startSession(
            SimulationSession(
                mode = SimulationMode.STATIONARY,
                status = SimulationStatus.RUNNING,
                latitude = latitude,
                longitude = longitude,
                bearingDegrees = 0f,
                speedMetersPerSecond = 0f,
                route = null,
                distanceTraveledMeters = 0.0,
            ),
        )
    }

    override fun startRoute(
        route: Route,
        speedMetersPerSecond: Float,
    ) {
        val start = route.geometry.firstOrNull() ?: (0.0 to 0.0)
        startSession(
            SimulationSession(
                mode = SimulationMode.ROUTE,
                status = SimulationStatus.RUNNING,
                latitude = start.first,
                longitude = start.second,
                bearingDegrees = 0f,
                speedMetersPerSecond = speedMetersPerSecond,
                route = route,
                distanceTraveledMeters = 0.0,
            ),
        )
    }

    override fun pause() {
        _session.update { current ->
            if (current?.status == SimulationStatus.RUNNING) current.copy(status = SimulationStatus.PAUSED) else current
        }
    }

    override fun resume() {
        _session.update { current ->
            if (current?.status == SimulationStatus.PAUSED) current.copy(status = SimulationStatus.RUNNING) else current
        }
    }

    override fun stop() {
        tickJob?.cancel()
        tickJob = null
        _session.value = null
        mockLocationPublisher.clear()
        context?.stopService(Intent(context, SimulationForegroundService::class.java))
    }

    private fun startSession(session: SimulationSession) {
        tickJob?.cancel()
        _session.value = session
        publishCurrent()
        context?.startForegroundService(Intent(context, SimulationForegroundService::class.java))
        tickJob =
            coroutineScope.launch {
                while (isActive) {
                    delay(tickIntervalMillis)
                    tick(tickIntervalMillis / 1_000.0)
                }
            }
    }

    internal fun tick(elapsedSeconds: Double) {
        val current = _session.value ?: return

        if (!mockLocationAuthorizationChecker.isAuthorized()) {
            stop()
            return
        }

        val updated =
            if (current.status == SimulationStatus.RUNNING && current.mode == SimulationMode.ROUTE) {
                val route = current.route ?: return
                val newDistance = current.distanceTraveledMeters + current.speedMetersPerSecond * elapsedSeconds
                val fix = routeProgressCalculator.interpolate(route, newDistance)
                current.copy(
                    status = if (fix.isRouteExhausted) SimulationStatus.COMPLETED else SimulationStatus.RUNNING,
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    bearingDegrees = fix.bearingDegrees,
                    distanceTraveledMeters = newDistance,
                )
            } else {
                current
            }

        _session.value = updated
        publishCurrent()
    }

    private fun publishCurrent() {
        val current = _session.value ?: return
        mockLocationPublisher.publish(
            current.latitude,
            current.longitude,
            current.bearingDegrees,
            current.speedMetersPerSecond,
        )
    }
}
