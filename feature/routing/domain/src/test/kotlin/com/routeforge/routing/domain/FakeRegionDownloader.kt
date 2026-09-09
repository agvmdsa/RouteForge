package com.routeforge.routing.domain

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.EmptyResult
import com.routeforge.coredomain.Result
import com.routeforge.routing.domain.model.Region

class FakeRegionDownloader : RegionDownloader {
    var result: EmptyResult<DataError.Network> = Result.Success(Unit)
    var progressSteps: List<Float> = listOf(1f)
    var lastDownloadedRegion: Region? = null

    override suspend fun download(
        region: Region,
        onProgress: (fraction: Float) -> Unit,
    ): EmptyResult<DataError.Network> {
        lastDownloadedRegion = region
        progressSteps.forEach(onProgress)
        return result
    }
}
