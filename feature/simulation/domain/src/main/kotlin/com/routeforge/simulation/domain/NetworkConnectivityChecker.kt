package com.routeforge.simulation.domain

interface NetworkConnectivityChecker {
    fun isConnected(): Boolean
}
