package com.routeforge.app

import android.content.Intent

internal object IncomingIntentReader {
    /** What the intent asks for, or `null` when it is not something this app opens (a launcher start, an unknown type). */
    fun read(intent: Intent?): IncomingRequest? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val data = intent.data ?: return null
        return when (data.scheme?.lowercase()) {
            "geo", "http", "https" -> IncomingRequest.Address(data.toString())
            else -> null
        }
    }
}
