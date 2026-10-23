package com.routeforge.coredomain.testing

import com.routeforge.coredomain.NetworkConnectivityChecker

class FakeNetworkConnectivityChecker : NetworkConnectivityChecker {
    var connected = true

    override fun isConnected(): Boolean = connected
}
