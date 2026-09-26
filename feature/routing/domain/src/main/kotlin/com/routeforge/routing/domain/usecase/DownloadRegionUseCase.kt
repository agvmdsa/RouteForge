package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.EmptyResult
import com.routeforge.coredomain.Result
import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.RegionDownloader

class DownloadRegionUseCase(
    private val regionDownloader: RegionDownloader,
    private val regionCatalog: RegionCatalog,
    private val recordRegionUsage: RecordRegionUsageUseCase,
    private val enforceStorageQuota: EnforceStorageQuotaUseCase,
) {
    suspend operator fun invoke(
        regionId: String,
        onProgress: (Float) -> Unit,
    ): EmptyResult<DataError.Network> {
        val region =
            regionCatalog.regionById(regionId)
                ?: return Result.Error(DataError.Network.NOT_FOUND)
        val result = regionDownloader.download(region, onProgress)
        if (result is Result.Success) {
            recordRegionUsage(setOf(regionId))
            enforceStorageQuota()
        }
        return result
    }
}
