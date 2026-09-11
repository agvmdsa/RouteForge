package com.routeforge.simulation.presentation

import com.routeforge.simulation.domain.NetworkConnectivityChecker

class FakeNetworkConnectivityChecker : NetworkConnectivityChecker {
    var connected = true

    override fun isConnected(): Boolean = connected
}
