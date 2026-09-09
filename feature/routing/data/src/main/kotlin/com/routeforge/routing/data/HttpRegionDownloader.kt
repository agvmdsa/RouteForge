package com.routeforge.routing.data

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.EmptyResult
import com.routeforge.coredomain.Result
import com.routeforge.routing.domain.RegionDownloader
import com.routeforge.routing.domain.model.Region
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException

private const val SEGMENT_BASE_URL = "https://brouter.de/brouter/segments4/"

class HttpRegionDownloader(
    private val httpClient: HttpClient,
    private val regionCatalog: BundledRegionCatalog,
) : RegionDownloader {
    override suspend fun download(
        region: Region,
        onProgress: (fraction: Float) -> Unit,
    ): EmptyResult<DataError.Network> {
        val segmentDirectory = regionCatalog.segmentDirectory
        segmentDirectory.mkdirs()
        val tileCount = region.tileIds.size

        regionCatalog.markDownloading(region.id)
        try {
            region.tileIds.forEachIndexed { index, tileId ->
                val result =
                    downloadTile(segmentDirectory, tileId) { tileFraction ->
                        onProgress((index + tileFraction) / tileCount)
                    }
                if (result is Result.Error) return result
            }

            onProgress(1f)
            return Result.Success(Unit)
        } finally {
            regionCatalog.markDownloadFinished(region.id)
        }
    }

    private suspend fun downloadTile(
        segmentDirectory: File,
        tileId: String,
        onTileProgress: (Float) -> Unit,
    ): EmptyResult<DataError.Network> =
        try {
            val response: HttpResponse =
                httpClient.get(SEGMENT_BASE_URL + tileId) {
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            onTileProgress(bytesSentTotal.toFloat() / contentLength)
                        }
                    }
                }
            if (response.status.isSuccess()) {
                File(segmentDirectory, tileId).writeBytes(response.body())
                Result.Success(Unit)
            } else {
                Result.Error(response.status.toNetworkError())
            }
        } catch (e: IOException) {
            Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Network.UNKNOWN)
        }
}

private fun HttpStatusCode.toNetworkError(): DataError.Network =
    when (value) {
        401 -> DataError.Network.UNAUTHORIZED
        403 -> DataError.Network.FORBIDDEN
        404 -> DataError.Network.NOT_FOUND
        408 -> DataError.Network.REQUEST_TIMEOUT
        409 -> DataError.Network.CONFLICT
        413 -> DataError.Network.PAYLOAD_TOO_LARGE
        429 -> DataError.Network.TOO_MANY_REQUESTS
        in 500..599 -> DataError.Network.SERVER_ERROR
        else -> DataError.Network.UNKNOWN
    }
