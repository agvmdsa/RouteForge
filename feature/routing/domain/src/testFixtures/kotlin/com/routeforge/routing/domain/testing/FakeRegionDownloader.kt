package com.routeforge.routing.domain.testing

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.EmptyResult
import com.routeforge.coredomain.Result
import com.routeforge.coredomain.model.Region
import com.routeforge.routing.domain.RegionDownloader
import kotlinx.coroutines.CompletableDeferred

class FakeRegionDownloader : RegionDownloader {
    var result: EmptyResult<DataError.Network> = Result.Success(Unit)
    var progressSteps: List<Float?> = listOf(1f)
    var lastDownloadedRegion: Region? = null
    private var gate: CompletableDeferred<Unit>? = null

    /** Makes the next download stop after its first progress step until [resume] is called. */
    fun pauseNextDownload() {
        gate = CompletableDeferred()
    }

    fun resume() {
        gate?.complete(Unit)
    }

    override suspend fun download(
        region: Region,
        onProgress: (fraction: Float?) -> Unit,
    ): EmptyResult<DataError.Network> {
        lastDownloadedRegion = region
        progressSteps.forEachIndexed { index, fraction ->
            onProgress(fraction)
            if (index == 0) gate?.await()
        }
        return result
    }
}
