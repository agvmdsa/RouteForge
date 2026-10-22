package com.routeforge.mocklocationsetup.data

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

internal fun resolveSettingsAction(
    primaryAction: String,
    isActionResolvable: (String) -> Boolean,
): String = if (isActionResolvable(primaryAction)) primaryAction else Settings.ACTION_SETTINGS

internal class SetupDeepLinkIntentFactory(
    private val packageManager: PackageManager,
) {
    private val isActionResolvable: (String) -> Boolean = { action ->
        Intent(action).resolveActivity(packageManager) != null
    }

    fun developerOptionsDeepLinkIntent(): Intent = Intent(resolveSettingsAction(Settings.ACTION_DEVICE_INFO_SETTINGS, isActionResolvable))

    fun mockLocationAppDeepLinkIntent(): Intent =
        Intent(resolveSettingsAction(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, isActionResolvable))
}
