package com.routeforge.coredomain.link

import com.routeforge.coredomain.model.RoutePoint

/** What one address (a link) means. */
internal sealed interface ExternalLink {
    /** One position, with the place's name when the address carries it. */
    data class Place(
        val point: RoutePoint,
        val name: String?,
    ) : ExternalLink

    /** The stops of a trip, in order; at least two. */
    data class Route(
        val stops: List<RoutePoint>,
    ) : ExternalLink

    /** A shortened address that only means something once its redirects are followed. */
    data class ShortLink(
        val url: String,
    ) : ExternalLink

    /** Not usable: no position, an invalid one, or a shape RouteForge does not read. */
    data object None : ExternalLink
}
