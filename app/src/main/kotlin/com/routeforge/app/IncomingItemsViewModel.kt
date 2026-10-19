package com.routeforge.app

import androidx.lifecycle.ViewModel
import com.routeforge.coredomain.link.ExternalLink
import com.routeforge.coredomain.link.ExternalLinkParser
import com.routeforge.coredomain.link.SharedTextOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Holds what another app sent until a tab is on screen to act on it. A view model, so that rebuilding the screen (a rotation, a
 * language change) neither loses it nor shows it again.
 */
class IncomingItemsViewModel : ViewModel() {
    private val _pending = MutableStateFlow<IncomingResult?>(null)
    val pending: StateFlow<IncomingResult?> = _pending.asStateFlow()

    /** A newer request replaces one that was not acted on yet. */
    fun submit(request: IncomingRequest) {
        _pending.value =
            when (request) {
                is IncomingRequest.Address -> IncomingResult.Outcome(outcomeOf(request.value), wasAddress = true)
            }
    }

    /** Takes the pending result, leaving none behind, so that it is acted on exactly once. */
    fun consume(): IncomingResult? = _pending.getAndUpdate { null }

    private fun outcomeOf(address: String): SharedTextOutcome =
        when (val link = ExternalLinkParser.parse(address)) {
            is ExternalLink.Place -> SharedTextOutcome.Place(link.point, link.name)
            else -> SharedTextOutcome.NotFound
        }
}
