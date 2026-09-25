package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.FakeSimulationController
import com.routeforge.simulation.domain.model.ExecutionMode
import com.routeforge.simulation.domain.model.JoystickInterruptDecision
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession
import com.routeforge.simulation.domain.model.SimulationStatus
import com.routeforge.simulation.domain.model.SpeedSetting
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private fun routeSession(status: SimulationStatus) =
    SimulationSession(
        mode = SimulationMode.ROUTE,
        status = status,
        latitude = 0.0,
        longitude = 0.0,
        bearingDegrees = 0f,
        speedMetersPerSecond = 1f,
        route = null,
        distanceTraveledMeters = 0.0,
        speedSetting = SpeedSetting.DEFAULT,
        executionMode = ExecutionMode.Once,
    )

class RequestJoystickInterruptUseCaseTest {
    private val controller = FakeSimulationController()
    private val useCase = RequestJoystickInterruptUseCase(controller)

    @Test
    fun `no active session proceeds immediately`() {
        assertEquals(JoystickInterruptDecision.ProceedImmediately, useCase())
    }

    @Test
    fun `a stationary session proceeds immediately`() {
        controller.emit(routeSession(SimulationStatus.RUNNING).copy(mode = SimulationMode.STATIONARY))

        assertEquals(JoystickInterruptDecision.ProceedImmediately, useCase())
    }

    @Test
    fun `a running route requires confirmation`() {
        controller.emit(routeSession(SimulationStatus.RUNNING))

        assertEquals(JoystickInterruptDecision.ConfirmationRequired, useCase())
    }

    @Test
    fun `a paused route requires confirmation`() {
        controller.emit(routeSession(SimulationStatus.PAUSED))

        assertEquals(JoystickInterruptDecision.ConfirmationRequired, useCase())
    }

    @Test
    fun `a completed route proceeds immediately`() {
        controller.emit(routeSession(SimulationStatus.COMPLETED))

        assertEquals(JoystickInterruptDecision.ProceedImmediately, useCase())
    }
}
