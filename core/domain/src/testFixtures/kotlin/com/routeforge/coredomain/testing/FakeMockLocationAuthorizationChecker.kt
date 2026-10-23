package com.routeforge.coredomain.testing

import com.routeforge.coredomain.MockLocationAuthorizationChecker

class FakeMockLocationAuthorizationChecker(
    var authorized: Boolean = true,
) : MockLocationAuthorizationChecker {
    override fun isAuthorized(): Boolean = authorized
}
