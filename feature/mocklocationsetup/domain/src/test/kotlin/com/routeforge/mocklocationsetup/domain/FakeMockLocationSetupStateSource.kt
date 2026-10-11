package com.routeforge.mocklocationsetup.domain

import android.content.Intent

class FakeMockLocationSetupStateSource(
    var developerOptionsEnabled: Boolean = false,
    var appSelectedAsMockLocationProvider: Boolean = false,
    var restrictedByPolicy: Boolean = false,
    var developerOptionsDeepLink: Intent? = null,
    var mockLocationAppDeepLink: Intent? = null,
) : MockLocationSetupStateSource {
    override fun isDeveloperOptionsEnabled(): Boolean = developerOptionsEnabled

    override fun isAppSelectedAsMockLocationProvider(): Boolean = appSelectedAsMockLocationProvider

    override fun isDeveloperOptionsRestrictedByPolicy(): Boolean = restrictedByPolicy

    override fun developerOptionsDeepLinkIntent(): Intent? = developerOptionsDeepLink

    override fun mockLocationAppDeepLinkIntent(): Intent? = mockLocationAppDeepLink
}
