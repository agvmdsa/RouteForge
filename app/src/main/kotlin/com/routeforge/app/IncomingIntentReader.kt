package com.routeforge.app

import android.content.Intent
import android.net.Uri

private const val PLAIN_TEXT = "text/plain"
private val GPX_TYPES = setOf("application/gpx+xml", "application/gpx")

internal object IncomingIntentReader {
    /** What the intent asks for, or `null` when it is not something this app opens (a launcher start, an unknown type). */
    fun read(intent: Intent?): IncomingRequest? =
        when (intent?.action) {
            Intent.ACTION_VIEW -> readView(intent)
            Intent.ACTION_SEND -> readSharedText(intent)
            else -> null
        }

    private fun readView(intent: Intent): IncomingRequest? {
        val data = intent.data ?: return null
        val scheme = data.scheme?.lowercase()
        return when {
            isGpx(intent.type, data) -> IncomingRequest.GpxFile(data)
            scheme == "geo" || scheme == "http" || scheme == "https" -> IncomingRequest.Address(data.toString())
            else -> null
        }
    }

    private fun isGpx(
        type: String?,
        data: Uri,
    ): Boolean {
        val isFile = data.scheme.equals("file", ignoreCase = true) || data.scheme.equals("content", ignoreCase = true)
        return type?.lowercase() in GPX_TYPES || (isFile && data.path.orEmpty().endsWith(".gpx", ignoreCase = true))
    }

    private fun readSharedText(intent: Intent): IncomingRequest? {
        if (intent.type?.startsWith(PLAIN_TEXT, ignoreCase = true) != true) return null
        return intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.let { IncomingRequest.SharedText(it.toString()) }
    }
}
