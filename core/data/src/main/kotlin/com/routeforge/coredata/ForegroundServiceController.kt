package com.routeforge.coredata

/** Starts/stops a foreground service, abstracted away from any specific `Service` subclass so a
 *  controller that needs one (region downloads, mock-location simulation) can be constructed in a
 *  unit test without a real Android [android.content.Context]. */
interface ForegroundServiceController {
    fun start()

    fun stop()
}

/** Does nothing — the default for controllers constructed without a real foreground service to
 *  manage, e.g. in tests. */
object NoOpForegroundServiceController : ForegroundServiceController {
    override fun start() = Unit

    override fun stop() = Unit
}
