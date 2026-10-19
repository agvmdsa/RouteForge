package com.routeforge.app

import com.routeforge.coredomain.link.SharedTextOutcome

/** Something another app asked RouteForge to open or take. */
sealed interface IncomingRequest {
    /** A `geo:` or web link opened with "Open with". */
    data class Address(
        val value: String,
    ) : IncomingRequest
}

/** What came out of reading a request, kept until a tab is on screen to act on it. */
sealed interface IncomingResult {
    /** A place, a route, a search or nothing; which action it becomes depends on the tab the user is on. */
    data class Outcome(
        val outcome: SharedTextOutcome,
        val wasAddress: Boolean,
    ) : IncomingResult
}
