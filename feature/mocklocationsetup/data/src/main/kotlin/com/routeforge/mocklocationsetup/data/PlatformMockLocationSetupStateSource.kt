package com.routeforge.mocklocationsetup.data

import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.provider.Settings
import com.routeforge.coredomain.MockLocationAuthorizationChecker
import com.routeforge.mocklocationsetup.domain.MockLocationSetupStateSource

class PlatformMockLocationSetupStateSource(
    private val context: Context,
    private val deepLinkIntentFactory: SetupDeepLinkIntentFactory,
    private val mockLocationAuthorizationChecker: MockLocationAuthorizationChecker,
) : MockLocationSetupStateSource {
    override fun isDeveloperOptionsEnabled(): Boolean =
        Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED,
            0,
        ) != 0

    override fun isAppSelectedAsMockLocationProvider(): Boolean {
        if (!isDeveloperOptionsEnabled()) return false
        return mockLocationAuthorizationChecker.isAuthorized()
    }

    override fun isDeveloperOptionsRestrictedByPolicy(): Boolean {
        val userManager = context.getSystemService(UserManager::class.java)
        return userManager.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)
    }

    override fun developerOptionsDeepLinkIntent(): Intent = deepLinkIntentFactory.developerOptionsDeepLinkIntent()

    override fun mockLocationAppDeepLinkIntent(): Intent = deepLinkIntentFactory.mockLocationAppDeepLinkIntent()
}
