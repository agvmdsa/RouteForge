package com.routeforge.simulation.domain.usecase

import com.routeforge.coredomain.NetworkConnectivityChecker

class IsNetworkConnectedUseCase(
    private val networkConnectivityChecker: NetworkConnectivityChecker,
) {
    operator fun invoke(): Boolean = networkConnectivityChecker.isConnected()
}
