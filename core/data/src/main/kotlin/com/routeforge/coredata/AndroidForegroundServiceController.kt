package com.routeforge.coredata

import android.app.Service
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "ForegroundServiceController"

/** Starts/stops [serviceClass] as a foreground service via [context]. */
class AndroidForegroundServiceController(
    private val context: Context,
    private val serviceClass: Class<out Service>,
) : ForegroundServiceController {
    override fun start() {
        try {
            context.startForegroundService(Intent(context, serviceClass))
        } catch (e: IllegalStateException) {
            // Android 12+ throws ForegroundServiceStartNotAllowedException (a subclass of this) when
            // the OS no longer allows starting a foreground service from the app's current state
            // (e.g. resumed outside a background-start exemption window). The caller's own work
            // (region download, simulation tick loop) keeps running either way — losing the
            // foreground service only means the OS can kill the process sooner, which beats crashing
            // outright.
            Log.w(TAG, "Couldn't start foreground service", e)
        }
    }

    override fun stop() {
        context.stopService(Intent(context, serviceClass))
    }
}
