package com.routeforge.coredata

import android.content.Context
import android.location.LocationManager
import android.location.provider.ProviderProperties
import com.routeforge.coredomain.MockLocationAuthorizationChecker

private const val MOCK_LOCATION_PROBE_PROVIDER = "com.routeforge.coredata.probe"

class PlatformMockLocationAuthorizationChecker(
    private val context: Context,
) : MockLocationAuthorizationChecker {
    override fun isAuthorized(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            addMockLocationProbeProvider(locationManager)
            locationManager.removeTestProvider(MOCK_LOCATION_PROBE_PROVIDER)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun addMockLocationProbeProvider(locationManager: LocationManager) {
        locationManager.addTestProvider(
            MOCK_LOCATION_PROBE_PROVIDER,
            false,
            false,
            false,
            false,
            true,
            true,
            true,
            ProviderProperties.POWER_USAGE_LOW,
            ProviderProperties.ACCURACY_FINE,
        )
    }
}
