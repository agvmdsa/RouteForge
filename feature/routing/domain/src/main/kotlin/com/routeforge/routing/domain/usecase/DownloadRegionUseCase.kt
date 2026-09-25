package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.EmptyResult
import com.routeforge.coredomain.Result
import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.RegionDownloader

class DownloadRegionUseCase(
    private val regionDownloader: RegionDownloader,
    private val regionCatalog: RegionCatalog,
) {
    suspend operator fun invoke(
        regionId: String,
        onProgress: (Float) -> Unit,
    ): EmptyResult<DataError.Network> {
        val region =
            regionCatalog.regionById(regionId)
                ?: return Result.Error(DataError.Network.NOT_FOUND)
        return regionDownloader.download(region, onProgress)
    }
}
