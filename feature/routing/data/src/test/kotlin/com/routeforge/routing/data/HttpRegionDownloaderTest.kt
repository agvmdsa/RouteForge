package com.routeforge.routing.data

import com.routeforge.coredomain.DataError
import com.routeforge.coredomain.Result
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File

private fun emptyAssetOpener(path: String) =
    when (path) {
        "regions-manifest.json" -> ByteArrayInputStream("[]".toByteArray())
        else -> ByteArrayInputStream(ByteArray(0))
    }

private val testRegion =
    Region(
        id = "test-region",
        displayName = "Test Region",
        minLatitude = 0.0,
        minLongitude = 0.0,
        maxLatitude = 1.0,
        maxLongitude = 1.0,
        tileIds = listOf("tile.rd5"),
        approximateSizeBytes = 5L,
        status = RegionStatus.NOT_DOWNLOADED,
    )

class HttpRegionDownloaderTest {
    @Test
    fun `download writes tile bytes to the segment directory and reports completed progress`(
        @TempDir segmentDir: File,
        @TempDir profileDir: File,
    ) = runTest {
        val tileBytes = byteArrayOf(1, 2, 3, 4, 5)
        val mockEngine =
            MockEngine {
                respond(
                    content = tileBytes,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentLength, tileBytes.size.toString()),
                )
            }
        val regionCatalog = BundledRegionCatalog(segmentDir, profileDir, ::emptyAssetOpener)
        val downloader = HttpRegionDownloader(HttpClient(mockEngine), regionCatalog)
        val progressValues = mutableListOf<Float>()

        val result = downloader.download(testRegion) { progressValues.add(it) }

        assertEquals(Result.Success(Unit), result)
        assertArrayEquals(tileBytes, File(segmentDir, "tile.rd5").readBytes())
        assertEquals(1f, progressValues.last())
    }

    @Test
    fun `download returns a mapped network error when the server responds with a failure status`(
        @TempDir segmentDir: File,
        @TempDir profileDir: File,
    ) = runTest {
        val mockEngine = MockEngine { respond(content = ByteArray(0), status = HttpStatusCode.NotFound) }
        val regionCatalog = BundledRegionCatalog(segmentDir, profileDir, ::emptyAssetOpener)
        val downloader = HttpRegionDownloader(HttpClient(mockEngine), regionCatalog)

        val result = downloader.download(testRegion) {}

        assertEquals(Result.Error(DataError.Network.NOT_FOUND), result)
    }
}
