package com.routeforge.mocklocationsetup.presentation

import android.provider.Settings
import com.routeforge.mocklocationsetup.domain.model.SetupShortcut
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SetupShortcutIntentTest {
    @Test
    fun `falls back to ACTION_SETTINGS when the primary target is unresolvable`() {
        val action = resolveSettingsAction(Settings.ACTION_DEVICE_INFO_SETTINGS) { false }

        assertEquals(Settings.ACTION_SETTINGS, action)
    }

    @Test
    fun `keeps the primary action when it resolves`() {
        val action = resolveSettingsAction(Settings.ACTION_DEVICE_INFO_SETTINGS) { true }

        assertEquals(Settings.ACTION_DEVICE_INFO_SETTINGS, action)
    }

    @Test
    fun `the device information shortcut opens the device information settings`() {
        assertEquals(Settings.ACTION_DEVICE_INFO_SETTINGS, SetupShortcut.DEVICE_INFO.settingsAction())
    }

    @Test
    fun `the developer options shortcut opens the application development settings`() {
        assertEquals(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, SetupShortcut.DEVELOPER_OPTIONS.settingsAction())
    }
}
