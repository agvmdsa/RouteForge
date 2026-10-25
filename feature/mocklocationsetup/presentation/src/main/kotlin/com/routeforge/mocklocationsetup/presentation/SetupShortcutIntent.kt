package com.routeforge.mocklocationsetup.presentation

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.routeforge.mocklocationsetup.domain.model.SetupShortcut

internal fun SetupShortcut.settingsAction(): String =
    when (this) {
        SetupShortcut.DEVICE_INFO -> Settings.ACTION_DEVICE_INFO_SETTINGS
        SetupShortcut.DEVELOPER_OPTIONS -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
    }

internal fun resolveSettingsAction(
    primaryAction: String,
    isActionResolvable: (String) -> Boolean,
): String = if (isActionResolvable(primaryAction)) primaryAction else Settings.ACTION_SETTINGS

internal fun SetupShortcut.toIntent(packageManager: PackageManager): Intent =
    Intent(
        resolveSettingsAction(settingsAction()) { action -> Intent(action).resolveActivity(packageManager) != null },
    )
