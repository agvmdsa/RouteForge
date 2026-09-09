package com.routeforge.routing.data

import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.model.Region
import com.routeforge.routing.domain.model.RegionStatus
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream

class BundledRegionCatalog(
    val segmentDirectory: File,
    private val profileDirectory: File,
    private val openAsset: (String) -> InputStream,
) : RegionCatalog {
    private val json = Json { ignoreUnknownKeys = true }
    private val downloadingRegionIds = mutableSetOf<String>()

    private val manifest: List<RegionManifestEntry> by lazy { loadManifest() }

    val profileFile: File by lazy {
        segmentDirectory.mkdirs()
        profileDirectory.mkdirs()
        copyAssetIfMissing("profile/car-fast.brf", File(profileDirectory, "car-fast.brf"))
        copyAssetIfMissing("profile/lookups.dat", File(profileDirectory, "lookups.dat"))
        File(profileDirectory, "car-fast.brf")
    }

    init {
        profileFile
    }

    override fun listRegions(): List<Region> = manifest.map { it.toRegion(currentStatus(it)) }

    override fun regionContaining(
        latitude: Double,
        longitude: Double,
    ): Region? = listRegions().firstOrNull { it.contains(latitude, longitude) }

    fun markDownloading(regionId: String) {
        downloadingRegionIds.add(regionId)
    }

    fun markDownloadFinished(regionId: String) {
        downloadingRegionIds.remove(regionId)
    }

    private fun currentStatus(entry: RegionManifestEntry): RegionStatus {
        if (entry.id in downloadingRegionIds) return RegionStatus.DOWNLOADING
        val allTilesPresent = entry.tileIds.isNotEmpty() && entry.tileIds.all { File(segmentDirectory, it).exists() }
        return if (allTilesPresent) RegionStatus.DOWNLOADED else RegionStatus.NOT_DOWNLOADED
    }

    private fun copyAssetIfMissing(
        assetPath: String,
        target: File,
    ) {
        if (target.exists()) return
        try {
            openAsset(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (_: IOException) {
        }
    }

    private fun loadManifest(): List<RegionManifestEntry> {
        val text = openAsset("regions-manifest.json").bufferedReader().use { it.readText() }
        return json.decodeFromString(text)
    }
}
