package com.routeforge.simulation.presentation

import com.routeforge.coredomain.MockLocationAuthorizationChecker
import com.routeforge.simulation.domain.usecase.ObserveAuthorizationLostUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.update

internal class MockLocationAuthorizationGuard(
    private val mockLocationAuthorizationChecker: MockLocationAuthorizationChecker,
    private val observeAuthorizationLost: ObserveAuthorizationLostUseCase,
) {
    internal fun isAuthorized(): Boolean = mockLocationAuthorizationChecker.isAuthorized()

    internal fun authorizationLost(): SharedFlow<Unit> = observeAuthorizationLost()

    internal fun reportUnauthorized(state: MutableStateFlow<SimulationState>) {
        state.update {
            it.copy(
                errorType = SimulationErrorType.NOT_AUTHORIZED,
                isBlockedByAuthorization = true,
            )
        }
    }
}
