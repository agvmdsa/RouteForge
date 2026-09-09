package com.routeforge.routing.data

import com.routeforge.routing.domain.model.RegionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

private val manifestJson =
    """
    [
      {
        "id": "downloadable-region",
        "displayName": "Downloadable Region",
        "minLatitude": 2.0,
        "minLongitude": 2.0,
        "maxLatitude": 3.0,
        "maxLongitude": 3.0,
        "tileIds": ["downloadable.rd5"],
        "approximateSizeBytes": 20
      }
    ]
    """.trimIndent()

private fun fakeAssetOpener(assetPath: String): InputStream =
    when (assetPath) {
        "regions-manifest.json" -> ByteArrayInputStream(manifestJson.toByteArray())
        else -> ByteArrayInputStream(ByteArray(0))
    }

class BundledRegionCatalogTest {
    @Test
    fun `region not yet downloaded reports not downloaded until its tile exists on disk`(
        @TempDir segmentDir: File,
        @TempDir profileDir: File,
    ) {
        val catalog = BundledRegionCatalog(segmentDir, profileDir, ::fakeAssetOpener)

        val beforeDownload = catalog.listRegions().first { it.id == "downloadable-region" }
        assertEquals(RegionStatus.NOT_DOWNLOADED, beforeDownload.status)

        File(segmentDir, "downloadable.rd5").writeBytes(byteArrayOf(1, 2, 3))
        val afterDownload = catalog.listRegions().first { it.id == "downloadable-region" }

        assertEquals(RegionStatus.DOWNLOADED, afterDownload.status)
    }

    @Test
    fun `marking a region as downloading reports downloading status until marked finished`(
        @TempDir segmentDir: File,
        @TempDir profileDir: File,
    ) {
        val catalog = BundledRegionCatalog(segmentDir, profileDir, ::fakeAssetOpener)

        catalog.markDownloading("downloadable-region")
        assertEquals(
            RegionStatus.DOWNLOADING,
            catalog.listRegions().first { it.id == "downloadable-region" }.status,
        )

        catalog.markDownloadFinished("downloadable-region")
        assertEquals(
            RegionStatus.NOT_DOWNLOADED,
            catalog.listRegions().first { it.id == "downloadable-region" }.status,
        )
    }
}
