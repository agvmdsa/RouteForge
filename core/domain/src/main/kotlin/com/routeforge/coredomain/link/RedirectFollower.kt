package com.routeforge.coredomain.link

import com.routeforge.coredomain.Result
import kotlinx.coroutines.withTimeoutOrNull

private val GOOGLE_HOSTS =
    setOf("maps.app.goo.gl", "goo.gl", "google.com", "www.google.com", "maps.google.com", "consent.google.com")
private val HTTPS_HOST = Regex("""^https://([^/?#]+)""", RegexOption.IGNORE_CASE)

/**
 * Follows a chain of redirects, within limits: [maxRequests] requests and [timeoutMillis] for the whole chain, and only
 * through `https` addresses on Google's own hosts, so that no other site learns that this device followed the link.
 */
class RedirectFollower(
    private val source: RedirectSource,
    private val maxRequests: Int = 5,
    private val timeoutMillis: Long = 5_000,
) {
    /** The first address along the chain, [start] included, that satisfies [isUseful]; null when the chain gives none. */
    suspend fun follow(
        start: String,
        isUseful: (String) -> Boolean,
    ): String? {
        if (isUseful(start)) return start
        return withTimeoutOrNull(timeoutMillis) { walk(start, isUseful) }
    }

    private suspend fun walk(
        start: String,
        isUseful: (String) -> Boolean,
    ): String? {
        val seen = mutableSetOf<String>()
        var current = start
        repeat(maxRequests) {
            if (!mayBeRequested(current) || !seen.add(current)) return null
            val next = (source.locationOf(current) as? Result.Success)?.data ?: return null
            if (isUseful(next)) return next
            current = next
        }
        return null
    }

    private fun mayBeRequested(address: String) = HTTPS_HOST.find(address)?.groupValues?.get(1)?.lowercase() in GOOGLE_HOSTS
}
