package com.routeforge.coredata

import android.app.Service
import android.content.Context
import android.content.Intent

/** Starts/stops [serviceClass] as a foreground service via [context]. */
class AndroidForegroundServiceController(
    private val context: Context,
    private val serviceClass: Class<out Service>,
) : ForegroundServiceController {
    override fun start() {
        context.startForegroundService(Intent(context, serviceClass))
    }

    override fun stop() {
        context.stopService(Intent(context, serviceClass))
    }
}
