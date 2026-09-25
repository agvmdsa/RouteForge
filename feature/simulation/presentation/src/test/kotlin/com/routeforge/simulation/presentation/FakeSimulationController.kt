package com.routeforge.simulation.presentation

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.ExecutionModeFailure
import com.routeforge.simulation.domain.JoystickStartFailure
import com.routeforge.simulation.domain.SimulationController
import com.routeforge.simulation.domain.model.ExecutionMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SpeedSetting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSimulationController : SimulationController {
    data class TeleportCall(
        val latitude: Double,
        val longitude: Double,
    )

    data class StartRouteCall(
        val route: Route,
        val speedSetting: SpeedSetting,
        val executionMode: ExecutionMode,
    )

    private val _mockedSession = MutableStateFlow<SimulationSession?>(null)
    override val mockedSession: StateFlow<SimulationSession?> = _mockedSession

    val teleportCalls = mutableListOf<TeleportCall>()
    val startRouteCalls = mutableListOf<StartRouteCall>()
    val setSpeedCalls = mutableListOf<SpeedSetting>()
    val setExecutionModeCalls = mutableListOf<ExecutionMode>()
    var pauseCallCount = 0
        private set
    var resumeCallCount = 0
        private set
    var stopCallCount = 0
        private set
    var startJoystickResult: Result<Unit, JoystickStartFailure> = Result.Success(Unit)
    var startJoystickCallCount = 0
        private set
    val updateJoystickDirectionCalls = mutableListOf<Float>()

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
        speedSetting: SpeedSetting,
        executionMode: ExecutionMode,
    ) {
        startRouteCalls.add(StartRouteCall(route, speedSetting, executionMode))
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

    override fun setSpeed(speed: SpeedSetting) {
        setSpeedCalls.add(speed)
    }

    override fun setExecutionMode(mode: ExecutionMode): Result<Unit, ExecutionModeFailure> {
        setExecutionModeCalls.add(mode)
        if (mode is ExecutionMode.Times && mode.count <= 0) return Result.Error(ExecutionModeFailure.NonPositiveCount)
        return Result.Success(Unit)
    }

    override fun startJoystick(): Result<Unit, JoystickStartFailure> {
        startJoystickCallCount++
        return startJoystickResult
    }

    override fun updateJoystickDirection(bearingDegrees: Float) {
        updateJoystickDirectionCalls.add(bearingDegrees)
    }
}
