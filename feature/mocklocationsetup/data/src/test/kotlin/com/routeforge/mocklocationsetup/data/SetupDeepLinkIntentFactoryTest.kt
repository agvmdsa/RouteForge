package com.routeforge.mocklocationsetup.data

import android.provider.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SetupDeepLinkIntentFactoryTest {
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
}
