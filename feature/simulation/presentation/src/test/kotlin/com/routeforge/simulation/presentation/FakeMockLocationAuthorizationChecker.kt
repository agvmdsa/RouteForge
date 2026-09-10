package com.routeforge.simulation.presentation

import com.routeforge.coredomain.MockLocationAuthorizationChecker

class FakeMockLocationAuthorizationChecker(
    var authorized: Boolean = true,
) : MockLocationAuthorizationChecker {
    override fun isAuthorized(): Boolean = authorized
}
