package com.routeforge.coredata

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.routeforge.coredomain.NetworkConnectivityChecker

class AndroidNetworkConnectivityChecker(
    private val context: Context,
) : NetworkConnectivityChecker {
    override fun isConnected(): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
