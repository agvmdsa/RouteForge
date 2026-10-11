package com.routeforge.mocklocationsetup.domain

import android.content.Intent

interface MockLocationSetupStateSource {
    fun isDeveloperOptionsEnabled(): Boolean

    fun isAppSelectedAsMockLocationProvider(): Boolean

    fun isDeveloperOptionsRestrictedByPolicy(): Boolean

    fun developerOptionsDeepLinkIntent(): Intent?

    fun mockLocationAppDeepLinkIntent(): Intent?
}
