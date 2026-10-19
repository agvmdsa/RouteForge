package com.routeforge.coredomain.link

import com.routeforge.coredomain.model.RoutePoint

/** Where a shared text or an opened address led. */
sealed interface SharedTextOutcome {
    data class Place(
        val point: RoutePoint,
        val name: String?,
    ) : SharedTextOutcome

    data class Route(
        val stops: List<RoutePoint>,
    ) : SharedTextOutcome

    /** No position was found; this is what to look for in the address search instead. */
    data class Search(
        val query: String,
    ) : SharedTextOutcome

    data object NotFound : SharedTextOutcome
}
