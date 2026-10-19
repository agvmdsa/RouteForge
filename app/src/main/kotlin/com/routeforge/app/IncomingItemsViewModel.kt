package com.routeforge.app

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.link.ResolveSharedTextUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch

/**
 * Holds what another app sent until a tab is on screen to act on it. A view model, so that rebuilding the screen (a rotation, a
 * language change) neither loses it, nor shows it again, nor interrupts the lookup of a short link that is under way.
 */
class IncomingItemsViewModel(
    private val resolveSharedText: ResolveSharedTextUseCase,
    private val applicationContext: Context,
) : ViewModel() {
    private val _pending = MutableStateFlow<IncomingResult?>(null)
    val pending: StateFlow<IncomingResult?> = _pending.asStateFlow()

    private var lookup: Job? = null

    /** A newer request replaces one that was not acted on yet, and cancels a lookup that is still under way, so that its late
     *  answer cannot appear after the newer one. */
    fun submit(request: IncomingRequest) {
        lookup?.cancel()
        _pending.value = null
        when (request) {
            is IncomingRequest.Address -> resolve(request.value, wasAddress = true)
            is IncomingRequest.SharedText -> resolve(request.value, wasAddress = false)
        }
    }

    /** Takes the pending result, leaving none behind, so that it is acted on exactly once. */
    fun consume(): IncomingResult? = _pending.getAndUpdate { null }

    // A readable position is used at once, with no suspension and no network; only a short link takes time.
    private fun resolve(
        text: String,
        wasAddress: Boolean,
    ) {
        lookup =
            viewModelScope.launch {
                val outcome = resolveSharedText(text, onLookingUp = ::tellLookingUp)
                _pending.value = IncomingResult.Outcome(outcome, wasAddress)
            }
    }

    private fun tellLookingUp() {
        Toast.makeText(applicationContext, R.string.external_looking_up, Toast.LENGTH_SHORT).show()
    }
}
