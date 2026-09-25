package com.routeforge.simulation.domain.model

import com.routeforge.coredomain.Result
import com.routeforge.simulation.domain.ExecutionModeFailure
import com.routeforge.simulation.domain.FakeSimulationController
import com.routeforge.simulation.domain.usecase.SetExecutionModeUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExecutionModeTest {
    private val controller = FakeSimulationController()
    private val useCase = SetExecutionModeUseCase(controller)

    @Test
    fun `Times with a non-positive count is rejected`() {
        assertEquals(Result.Error(ExecutionModeFailure.NonPositiveCount), useCase(ExecutionMode.Times(0)))
        assertEquals(Result.Error(ExecutionModeFailure.NonPositiveCount), useCase(ExecutionMode.Times(-1)))
    }

    @Test
    fun `Times with a positive count is accepted`() {
        assertEquals(Result.Success(Unit), useCase(ExecutionMode.Times(1)))
        assertEquals(Result.Success(Unit), useCase(ExecutionMode.Times(5)))
    }

    @Test
    fun `Once and Loop are always accepted`() {
        assertEquals(Result.Success(Unit), useCase(ExecutionMode.Once))
        assertEquals(Result.Success(Unit), useCase(ExecutionMode.Loop))
    }
}
