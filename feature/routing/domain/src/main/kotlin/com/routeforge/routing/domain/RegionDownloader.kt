package com.routeforge.routing.domain

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.EmptyResult
import com.routeforge.routing.domain.model.Region

interface RegionDownloader {
    /** [onProgress] receives the running fraction (0f..1f) across all of [region]'s tiles, or
     *  `null` whenever the current tile's exact byte progress can't be determined (e.g. the
     *  server didn't report a Content-Length) — callers must treat `null` as "still working,
     *  progress unknown," never as "no change since last report." */
    suspend fun download(
        region: Region,
        onProgress: (fraction: Float?) -> Unit,
    ): EmptyResult<DataError.Network>
}
