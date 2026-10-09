package com.routeforge.routing.data

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.EmptyResult
import com.routeforge.coredomain.Result
import com.routeforge.routing.domain.RegionDownloader
import com.routeforge.coredomain.model.Region
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException

private const val SEGMENT_BASE_URL = "https://brouter.de/brouter/segments4/"
private const val DOWNLOAD_BUFFER_SIZE_BYTES = 8 * 1024

class HttpRegionDownloader(
    private val httpClient: HttpClient,
    private val regionCatalog: BundledRegionCatalog,
) : RegionDownloader {
    override suspend fun download(
        region: Region,
        onProgress: (fraction: Float?) -> Unit,
    ): EmptyResult<DataError.Network> {
        val segmentDirectory = regionCatalog.segmentDirectory
        segmentDirectory.mkdirs()
        val tileCount = region.tileIds.size

        regionCatalog.markDownloading(region.id)
        try {
            region.tileIds.forEachIndexed { index, tileId ->
                val result =
                    downloadTile(segmentDirectory, tileId) { tileFraction ->
                        onProgress(tileFraction?.let { (index + it) / tileCount })
                    }
                if (result is Result.Error) return result
            }

            onProgress(1f)
            return Result.Success(Unit)
        } finally {
            regionCatalog.markDownloadFinished(region.id)
        }
    }

    /** Uses `prepareGet(...).execute { }` (a genuinely streaming request) rather than plain
     *  `get(...)` — Ktor's `SaveBody` plugin, installed by default, unconditionally buffers the
     *  *entire* response body in memory for any non-streaming request before this code ever runs,
     *  regardless of whether the body is later read via `body()` or `bodyAsChannel()`. Only
     *  `HttpStatement.execute` skips that buffering, which is required for [writeChannelToFile]
     *  below to actually avoid materializing a whole (possibly huge) segment file in memory. */
    private suspend fun downloadTile(
        segmentDirectory: File,
        tileId: String,
        onTileProgress: (Float?) -> Unit,
    ): EmptyResult<DataError.Network> =
        try {
            httpClient
                .prepareGet(SEGMENT_BASE_URL + tileId) {
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            onTileProgress(bytesSentTotal.toFloat() / contentLength)
                        } else {
                            onTileProgress(null)
                        }
                    }
                }.execute { response: HttpResponse ->
                    if (response.status.isSuccess()) {
                        writeChannelToFile(response.bodyAsChannel(), File(segmentDirectory, tileId))
                        Result.Success(Unit)
                    } else {
                        Result.Error(response.status.toNetworkError())
                    }
                }
        } catch (e: IOException) {
            Result.Error(DataError.Network.NO_INTERNET)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Network.UNKNOWN)
        }

    /** Streams the response directly to disk in fixed-size chunks instead of materializing the
     *  whole tile in memory first — segment files can be large enough that reading one fully
     *  into memory risks an OutOfMemoryError. */
    private suspend fun writeChannelToFile(
        channel: ByteReadChannel,
        destination: File,
    ) {
        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE_BYTES)
        destination.outputStream().use { output ->
            while (true) {
                val bytesRead = channel.readAvailable(buffer)
                if (bytesRead == -1) break
                output.write(buffer, 0, bytesRead)
            }
        }
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
