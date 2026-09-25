package com.routeforge.simulation.domain

import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Route
import com.routeforge.simulation.domain.model.ExecutionMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SpeedSetting
import kotlinx.coroutines.flow.StateFlow

interface SimulationController {
    val mockedSession: StateFlow<SimulationSession?>

    fun teleport(
        latitude: Double,
        longitude: Double,
    )

    fun startRoute(
        route: Route,
        speedSetting: SpeedSetting,
        executionMode: ExecutionMode = ExecutionMode.DEFAULT,
    )

    fun pause()

    fun resume()

    fun stop()

    /** FR-016: takes effect immediately, whether a route is playing or the joystick is active. */
    fun setSpeed(speed: SpeedSetting)

    /** FR-020: rejects a non-positive [ExecutionMode.Times] count instead of applying it. */
    fun setExecutionMode(mode: ExecutionMode): Result<Unit, ExecutionModeFailure>

    /** FR-022/FR-026: starts from the last known real location, or continues from the current
     *  mocked position if one already exists (this is also how FR-025's route interrupt hands
     *  off control); fails only if no session exists yet and no real location fix has ever been
     *  observed. */
    fun startJoystick(): Result<Unit, JoystickStartFailure>

    /** Releasing the joystick is expressed as [pause] (stop moving, stay in place); dragging
     *  again is [resume] + a fresh [updateJoystickDirection] call. */
    fun updateJoystickDirection(bearingDegrees: Float)
}
