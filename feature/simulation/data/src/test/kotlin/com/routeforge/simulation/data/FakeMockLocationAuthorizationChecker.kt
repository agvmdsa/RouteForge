package com.routeforge.simulation.data

import com.routeforge.coredomain.MockLocationAuthorizationChecker

class FakeMockLocationAuthorizationChecker(
    var authorized: Boolean = true,
) : MockLocationAuthorizationChecker {
    override fun isAuthorized(): Boolean = authorized
}
