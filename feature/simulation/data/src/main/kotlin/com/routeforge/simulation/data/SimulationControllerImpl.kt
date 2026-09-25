package com.routeforge.simulation.data

import android.content.Context
import android.content.Intent
import com.routeforge.coredomain.GeoMath
import com.routeforge.coredomain.LastKnownRealLocationHolder
import com.routeforge.coredomain.MockLocationAuthorizationChecker
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.ExecutionModeFailure
import com.routeforge.simulation.domain.JoystickStartFailure
import com.routeforge.simulation.domain.MockLocationPublisher
import com.routeforge.simulation.domain.RouteProgressCalculator
import com.routeforge.simulation.domain.SimulationController
import com.routeforge.simulation.domain.model.ExecutionMode
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SimulationStatus
import com.routeforge.simulation.domain.model.SpeedSetting
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

private const val DEFAULT_TICK_INTERVAL_MILLIS = 100L

class SimulationControllerImpl(
    private val mockLocationPublisher: MockLocationPublisher,
    private val mockLocationAuthorizationChecker: MockLocationAuthorizationChecker,
    private val lastKnownRealLocationHolder: LastKnownRealLocationHolder = LastKnownRealLocationHolder(),
    private val context: Context? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val tickIntervalMillis: Long = DEFAULT_TICK_INTERVAL_MILLIS,
) : SimulationController {
    private val routeProgressCalculator = RouteProgressCalculator()

    private val _mockedSession = MutableStateFlow<SimulationSession?>(null)
    override val mockedSession: StateFlow<SimulationSession?> = _mockedSession.asStateFlow()

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
        speedSetting: SpeedSetting,
        executionMode: ExecutionMode,
    ) {
        val start = route.geometry.firstOrNull() ?: (0.0 to 0.0)
        startSession(
            SimulationSession(
                mode = SimulationMode.ROUTE,
                status = SimulationStatus.RUNNING,
                latitude = start.first,
                longitude = start.second,
                bearingDegrees = 0f,
                speedMetersPerSecond = speedSetting.metersPerSecond,
                route = route,
                distanceTraveledMeters = 0.0,
                speedSetting = speedSetting,
                executionMode = executionMode,
            ),
        )
    }

    override fun pause() {
        _mockedSession.update { current ->
            if (current?.status == SimulationStatus.RUNNING) current.copy(status = SimulationStatus.PAUSED) else current
        }
    }

    override fun resume() {
        _mockedSession.update { current ->
            if (current?.status == SimulationStatus.PAUSED) current.copy(status = SimulationStatus.RUNNING) else current
        }
    }

    override fun stop() {
        tickJob?.cancel()
        tickJob = null
        _mockedSession.value = null
        mockLocationPublisher.clear()
        context?.stopService(Intent(context, SimulationForegroundService::class.java))
    }

    override fun setSpeed(speed: SpeedSetting) {
        _mockedSession.update { current ->
            current?.copy(speedSetting = speed, speedMetersPerSecond = speed.metersPerSecond)
        }
    }

    override fun setExecutionMode(mode: ExecutionMode): Result<Unit, ExecutionModeFailure> {
        if (mode is ExecutionMode.Times && mode.count <= 0) {
            return Result.Error(ExecutionModeFailure.NonPositiveCount)
        }
        _mockedSession.update { it?.copy(executionMode = mode) }
        return Result.Success(Unit)
    }

    override fun startJoystick(): Result<Unit, JoystickStartFailure> {
        val current = _mockedSession.value
        val latitude: Double
        val longitude: Double
        if (current != null) {
            latitude = current.latitude
            longitude = current.longitude
        } else {
            val lastReal =
                lastKnownRealLocationHolder.location.value
                    ?: return Result.Error(JoystickStartFailure.NoRealLocationFixYet)
            latitude = lastReal.latitude
            longitude = lastReal.longitude
        }

        val speedSetting = current?.speedSetting ?: SpeedSetting.DEFAULT
        startSession(
            SimulationSession(
                mode = SimulationMode.JOYSTICK,
                status = SimulationStatus.RUNNING,
                latitude = latitude,
                longitude = longitude,
                bearingDegrees = current?.bearingDegrees ?: 0f,
                speedMetersPerSecond = speedSetting.metersPerSecond,
                route = null,
                distanceTraveledMeters = 0.0,
                speedSetting = speedSetting,
            ),
        )
        return Result.Success(Unit)
    }

    override fun updateJoystickDirection(bearingDegrees: Float) {
        _mockedSession.update { current ->
            if (current?.mode == SimulationMode.JOYSTICK) current.copy(bearingDegrees = bearingDegrees) else current
        }
    }

    private fun startSession(mockedSession: SimulationSession) {
        tickJob?.cancel()
        _mockedSession.value = mockedSession
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
        val current = _mockedSession.value ?: return

        if (!mockLocationAuthorizationChecker.isAuthorized()) {
            stop()
            return
        }

        val updated =
            when {
                current.status == SimulationStatus.RUNNING && current.mode == SimulationMode.ROUTE ->
                    tickRoute(current, elapsedSeconds)
                current.status == SimulationStatus.RUNNING && current.mode == SimulationMode.JOYSTICK ->
                    tickJoystick(current, elapsedSeconds)
                else -> current
            }

        _mockedSession.value = updated
        publishCurrent()
    }

    private fun tickRoute(
        current: SimulationSession,
        elapsedSeconds: Double,
    ): SimulationSession {
        val route = current.route ?: return current
        val newDistance = current.distanceTraveledMeters + current.speedMetersPerSecond * elapsedSeconds
        val fix = routeProgressCalculator.interpolate(route, newDistance)

        if (!fix.isRouteExhausted) {
            return current.copy(
                status = SimulationStatus.RUNNING,
                latitude = fix.latitude,
                longitude = fix.longitude,
                bearingDegrees = fix.bearingDegrees,
                distanceTraveledMeters = newDistance,
            )
        }

        val shouldRestart =
            when (val executionMode = current.executionMode) {
                ExecutionMode.Once -> false
                is ExecutionMode.Times -> current.completedRuns + 1 < executionMode.count
                ExecutionMode.Loop -> true
            }

        return if (shouldRestart) {
            val (startLat, startLon) = route.geometry.first()
            current.copy(
                status = SimulationStatus.RUNNING,
                latitude = startLat,
                longitude = startLon,
                distanceTraveledMeters = 0.0,
                completedRuns = current.completedRuns + 1,
            )
        } else {
            current.copy(
                status = SimulationStatus.COMPLETED,
                latitude = fix.latitude,
                longitude = fix.longitude,
                bearingDegrees = fix.bearingDegrees,
                distanceTraveledMeters = newDistance,
            )
        }
    }

    private fun tickJoystick(
        current: SimulationSession,
        elapsedSeconds: Double,
    ): SimulationSession {
        val distance = current.speedMetersPerSecond * elapsedSeconds
        if (distance <= 0.0) return current
        val (latitude, longitude) = GeoMath.destination(current.latitude, current.longitude, current.bearingDegrees, distance)
        return current.copy(latitude = latitude, longitude = longitude)
    }

    private fun publishCurrent() {
        val current = _mockedSession.value ?: return
        mockLocationPublisher.publish(
            current.latitude,
            current.longitude,
            current.bearingDegrees,
            current.speedMetersPerSecond,
        )
    }
}
