package com.routeforge.simulation.presentation

import com.routeforge.coredomain.testing.FakeMockLocationAuthorizationChecker
import com.routeforge.simulation.domain.testing.FakeSimulationController
import com.routeforge.simulation.domain.usecase.ObserveAuthorizationLostUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MockLocationAuthorizationGuardTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val controller = FakeSimulationController()
    private val checker = FakeMockLocationAuthorizationChecker()
    private val guard =
        MockLocationAuthorizationGuard(
            mockLocationAuthorizationChecker = checker,
            observeAuthorizationLost = ObserveAuthorizationLostUseCase(controller),
        )

    @Test
    fun `it reports whether the app is still the selected mock location app`() {
        assertTrue(guard.isAuthorized())

        checker.authorized = false

        assertFalse(guard.isAuthorized())
    }

    @Test
    fun `reporting an unauthorized state sets the error and blocks the screen`() {
        val state = MutableStateFlow(SimulationState())

        guard.reportUnauthorized(state)

        assertEquals(SimulationErrorType.NOT_AUTHORIZED, state.value.errorType)
        assertTrue(state.value.isBlockedByAuthorization)
    }

    @Test
    fun `reporting leaves the rest of the state alone`() {
        val state = MutableStateFlow(SimulationState(playbackSpeedKmh = 42f))

        guard.reportUnauthorized(state)

        assertEquals(42f, state.value.playbackSpeedKmh)
        assertNull(state.value.mockedSession)
    }

    @Test
    fun `it passes on the controller's report that the authorization was lost`() =
        runTest(dispatcher) {
            val lost = async { guard.authorizationLost().first() }

            controller.emitAuthorizationLost()

            assertEquals(Unit, lost.await())
        }
}
