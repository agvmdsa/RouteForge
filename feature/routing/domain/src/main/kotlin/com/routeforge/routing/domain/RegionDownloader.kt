package com.routeforge.routing.domain

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.EmptyResult
import com.routeforge.routing.domain.model.Region

interface RegionDownloader {
    suspend fun download(
        region: Region,
        onProgress: (fraction: Float) -> Unit,
    ): EmptyResult<DataError.Network>
}
