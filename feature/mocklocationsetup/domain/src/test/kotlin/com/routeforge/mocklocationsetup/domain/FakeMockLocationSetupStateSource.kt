package com.routeforge.mocklocationsetup.domain

class FakeMockLocationSetupStateSource(
    var developerOptionsEnabled: Boolean = false,
    var appSelectedAsMockLocationProvider: Boolean = false,
    var restrictedByPolicy: Boolean = false,
) : MockLocationSetupStateSource {
    override fun isDeveloperOptionsEnabled(): Boolean = developerOptionsEnabled

    override fun isAppSelectedAsMockLocationProvider(): Boolean = appSelectedAsMockLocationProvider

    override fun isDeveloperOptionsRestrictedByPolicy(): Boolean = restrictedByPolicy
}
