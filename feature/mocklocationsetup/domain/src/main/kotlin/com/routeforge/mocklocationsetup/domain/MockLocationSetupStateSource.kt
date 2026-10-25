package com.routeforge.mocklocationsetup.domain

interface MockLocationSetupStateSource {
    fun isDeveloperOptionsEnabled(): Boolean

    fun isAppSelectedAsMockLocationProvider(): Boolean

    fun isDeveloperOptionsRestrictedByPolicy(): Boolean
}
