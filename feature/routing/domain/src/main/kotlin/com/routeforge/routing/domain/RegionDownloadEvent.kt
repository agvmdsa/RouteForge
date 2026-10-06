package com.routeforge.routing.domain

/** One-shot notifications from [RegionDownloadController] for outcomes that aren't already
 *  reflected by [RegionDownloadController.state] going back to `null` (a plain success needs no
 *  event — the region catalog's next refresh already shows the new status). */
sealed interface RegionDownloadEvent {
    data class Failed(
        val regionId: String,
    ) : RegionDownloadEvent
}
