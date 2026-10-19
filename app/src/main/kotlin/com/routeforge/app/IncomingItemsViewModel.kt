package com.routeforge.app

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeforge.coredomain.link.ResolveSharedTextUseCase
import com.routeforge.coredomain.model.IncomingRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

private const val MAX_FILE_BYTES = 5 * 1024 * 1024
private const val READ_BUFFER_BYTES = 8 * 1024

/**
 * Holds what another app sent until a tab is on screen to act on it. A view model, so that rebuilding the screen (a rotation, a
 * language change) neither loses it, nor shows it again, nor interrupts the lookup of a short link or the reading of a file
 * that is under way.
 */
class IncomingItemsViewModel(
    private val resolveSharedText: ResolveSharedTextUseCase,
    private val applicationContext: Context,
) : ViewModel() {
    private val _pending = MutableStateFlow<IncomingResult?>(null)
    val pending: StateFlow<IncomingResult?> = _pending.asStateFlow()

    private var inFlight: Job? = null

    /** A newer request replaces one that was not acted on yet, and cancels work that is still under way (a lookup, a file being
     *  read), so that its late answer cannot appear after the newer one. */
    fun submit(request: IncomingRequest) {
        inFlight?.cancel()
        _pending.value = null
        when (request) {
            is IncomingRequest.Address -> resolve(request.value, wasAddress = true)
            is IncomingRequest.SharedText -> resolve(request.value, wasAddress = false)
            is IncomingRequest.GpxFile -> readFile(request.uri)
        }
    }

    /** Takes the pending result, leaving none behind, so that it is acted on exactly once. */
    fun consume(): IncomingResult? = _pending.getAndUpdate { null }

    // A readable position is used at once, with no suspension and no network; only a short link takes time.
    private fun resolve(
        text: String,
        wasAddress: Boolean,
    ) {
        inFlight =
            viewModelScope.launch {
                val outcome = resolveSharedText(text, onLookingUp = ::tellLookingUp)
                _pending.value = IncomingResult.Outcome(outcome, wasAddress)
            }
    }

    // Read at once: the permission to read the file lasts only as long as the request that brought it.
    private fun readFile(uri: Uri) {
        inFlight =
            viewModelScope.launch {
                val bytes = withContext(Dispatchers.IO) { readAtMost(uri, MAX_FILE_BYTES) }
                _pending.value =
                    if (bytes == null) {
                        IncomingResult.Message(R.string.external_file_unreadable)
                    } else {
                        IncomingResult.Route(IncomingRoute.GpxFile(bytes))
                    }
            }
    }

    private fun readAtMost(
        uri: Uri,
        limit: Int,
    ): ByteArray? =
        try {
            applicationContext.contentResolver.openInputStream(uri)?.use { it.readAtMost(limit) }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }

    private fun tellLookingUp() {
        Toast.makeText(applicationContext, R.string.external_looking_up, Toast.LENGTH_SHORT).show()
    }
}

/** Everything in the stream, or null when it is longer than [limit] bytes (reading stops there, so a huge file costs nothing). */
private fun InputStream.readAtMost(limit: Int): ByteArray? {
    val read = ByteArrayOutputStream()
    val buffer = ByteArray(READ_BUFFER_BYTES)
    while (true) {
        val count = read(buffer)
        if (count < 0) return read.toByteArray()
        if (read.size() + count > limit) return null
        read.write(buffer, 0, count)
    }
}
