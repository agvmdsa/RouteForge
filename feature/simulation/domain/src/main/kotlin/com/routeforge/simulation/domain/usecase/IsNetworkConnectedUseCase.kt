package com.routeforge.simulation.domain.usecase

import com.routeforge.simulation.domain.NetworkConnectivityChecker

class IsNetworkConnectedUseCase(
    private val networkConnectivityChecker: NetworkConnectivityChecker,
) {
    operator fun invoke(): Boolean = networkConnectivityChecker.isConnected()
}
