package com.routeforge.routing.domain

import com.routeforge.coredomain.model.Region
import com.routeforge.routing.domain.model.RegionDownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Owns the single in-flight region download independent of any ViewModel/NavBackStackEntry
 *  lifecycle, so it survives the user navigating away from the Region Catalog screen or
 *  minimizing the app (spec 006 FR-003/FR-004) — mirrors [com.routeforge.routing.domain.RegionDownloader]'s
 *  caller moving from a ViewModel-owned coroutine to this controller's own scope. */
interface RegionDownloadController {
    val state: StateFlow<RegionDownloadState?>
    val events: Flow<RegionDownloadEvent>

    /** No-op if a download is already active (`state.value != null`) — carries RF-047's
     *  reentrancy guard forward at the one place that can never be bypassed (spec FR-012). */
    fun start(region: Region)

    /** Cancels the in-flight download and discards everything downloaded so far for that
     *  region, per explicit product decision (spec FR-006). No-op if nothing is downloading. */
    fun cancel()
}
