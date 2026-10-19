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

/**
 * Finds what a shared text points at. A position written in the text is used as it is, without the network; only a short
 * Google Maps link that carries none is followed, and only when nothing else in the text was readable.
 */
class ResolveSharedTextUseCase(
    private val follower: RedirectFollower,
) {
    /** [onLookingUp] is called once, just before the first request, so that the caller can tell the user to wait. */
    suspend operator fun invoke(
        text: String,
        onLookingUp: () -> Unit = {},
    ): SharedTextOutcome {
        val shared = SharedText.parse(text)
        val links = shared.addresses.map { ExternalLinkParser.parse(it) }
        links.firstNotNullOfOrNull { outcomeOf(it, shared.name) }?.let { return it }

        val shortLink = links.filterIsInstance<ExternalLink.ShortLink>().firstOrNull()
        if (shortLink != null) {
            onLookingUp()
            val found = follower.follow(shortLink.url) { outcomeOf(ExternalLinkParser.parse(it), null) != null }
            found?.let { outcomeOf(ExternalLinkParser.parse(it), shared.name) }?.let { return it }
        }
        return shared.searchText?.let { SharedTextOutcome.Search(it) } ?: SharedTextOutcome.NotFound
    }

    private fun outcomeOf(
        link: ExternalLink,
        fallbackName: String?,
    ): SharedTextOutcome? =
        when (link) {
            is ExternalLink.Place -> SharedTextOutcome.Place(link.point, link.name ?: fallbackName)
            is ExternalLink.Route -> SharedTextOutcome.Route(link.stops)
            is ExternalLink.ShortLink, ExternalLink.None -> null
        }
}
