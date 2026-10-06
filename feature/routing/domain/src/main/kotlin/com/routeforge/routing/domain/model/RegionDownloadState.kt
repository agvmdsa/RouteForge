package com.routeforge.routing.domain.model

/** Snapshot of the single in-flight region download, owned by [com.routeforge.routing.domain.RegionDownloadController].
 *  [progress] is `null` whenever exact byte progress for the current tile can't be determined
 *  (e.g. the server didn't report a size) — callers must render that as "still working, unknown
 *  how far," never as "no change since last report." */
data class RegionDownloadState(
    val region: Region,
    val progress: Float?,
)
