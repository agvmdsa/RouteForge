package com.routeforge.simulation.domain.usecase

import com.routeforge.coredomain.Result
import com.routeforge.simulation.domain.FakeSimulationController
import com.routeforge.simulation.domain.JoystickStartFailure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfirmJoystickInterruptUseCaseTest {
    private val controller = FakeSimulationController()
    private val useCase = ConfirmJoystickInterruptUseCase(controller)

    @Test
    fun `delegates directly to the controller's startJoystick`() {
        controller.startJoystickResult = Result.Success(Unit)

        val result = useCase()

        assertEquals(Result.Success(Unit), result)
        assertEquals(1, controller.startJoystickCallCount)
    }

    @Test
    fun `propagates a failure from the controller unchanged`() {
        controller.startJoystickResult = Result.Error(JoystickStartFailure.NoRealLocationFixYet)

        val result = useCase()

        assertEquals(Result.Error(JoystickStartFailure.NoRealLocationFixYet), result)
    }
}
