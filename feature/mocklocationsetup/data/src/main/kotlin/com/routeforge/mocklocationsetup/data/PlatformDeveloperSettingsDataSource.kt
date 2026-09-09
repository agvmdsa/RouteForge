package com.routeforge.mocklocationsetup.data

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.UserManager
import android.provider.Settings
import com.routeforge.mocklocationsetup.domain.DeveloperSettingsDataSource

private const val MOCK_LOCATION_PROBE_PROVIDER = "com.routeforge.mocklocationsetup.probe"

class PlatformDeveloperSettingsDataSource(
    private val context: Context,
    private val deepLinkIntentFactory: SetupDeepLinkIntentFactory,
) : DeveloperSettingsDataSource {
    override fun isDeveloperOptionsEnabled(): Boolean =
        Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED,
            0,
        ) != 0

    override fun isAppSelectedAsMockLocationProvider(): Boolean {
        if (!isDeveloperOptionsEnabled()) return false

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            addMockLocationProbeProvider(locationManager)
            locationManager.removeTestProvider(MOCK_LOCATION_PROBE_PROVIDER)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun isDeveloperOptionsRestrictedByPolicy(): Boolean {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        return userManager.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)
    }

    override fun developerOptionsDeepLinkIntent(): Intent = deepLinkIntentFactory.developerOptionsDeepLinkIntent()

    override fun mockLocationAppDeepLinkIntent(): Intent = deepLinkIntentFactory.mockLocationAppDeepLinkIntent()

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
