package com.routeforge.app

import android.content.Intent

private const val PLAIN_TEXT = "text/plain"

internal object IncomingIntentReader {
    /** What the intent asks for, or `null` when it is not something this app opens (a launcher start, an unknown type). */
    fun read(intent: Intent?): IncomingRequest? =
        when (intent?.action) {
            Intent.ACTION_VIEW -> readLink(intent)
            Intent.ACTION_SEND -> readSharedText(intent)
            else -> null
        }

    private fun readLink(intent: Intent): IncomingRequest? {
        val data = intent.data ?: return null
        return when (data.scheme?.lowercase()) {
            "geo", "http", "https" -> IncomingRequest.Address(data.toString())
            else -> null
        }
    }

    private fun readSharedText(intent: Intent): IncomingRequest? {
        if (intent.type?.startsWith(PLAIN_TEXT, ignoreCase = true) != true) return null
        return intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.let { IncomingRequest.SharedText(it.toString()) }
    }
}
