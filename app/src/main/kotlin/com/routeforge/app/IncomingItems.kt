package com.routeforge.app

import android.net.Uri
import androidx.annotation.StringRes
import com.routeforge.coredomain.link.SharedTextOutcome
import com.routeforge.coredomain.model.IncomingRoute

/** Something another app asked RouteForge to open or take. */
sealed interface IncomingRequest {
    /** A `geo:` or web link opened with "Open with". */
    data class Address(
        val value: String,
    ) : IncomingRequest

    /** Text shared with RouteForge from another app's Share sheet. */
    data class SharedText(
        val value: String,
    ) : IncomingRequest

    /** A `.gpx` file opened with "Open with"; the permission to read it lasts only as long as the request. */
    data class GpxFile(
        val uri: Uri,
    ) : IncomingRequest
}

/** What came out of reading a request, kept until a tab is on screen to act on it. */
sealed interface IncomingResult {
    /** A place, a route, a search or nothing; which action it becomes depends on the tab the user is on. */
    data class Outcome(
        val outcome: SharedTextOutcome,
        val wasAddress: Boolean,
    ) : IncomingResult

    /** A route that goes to Plan Route whichever tab the user is on. */
    class Route(
        val route: IncomingRoute,
    ) : IncomingResult

    /** Nothing to act on, only something to tell the user. */
    data class Message(
        @StringRes val text: Int,
    ) : IncomingResult
}
