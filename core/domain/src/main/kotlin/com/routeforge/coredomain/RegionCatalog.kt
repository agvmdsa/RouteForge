package com.routeforge.coredomain

import com.routeforge.coredomain.model.Region

enum class RegionDeletionResult { FULLY_DELETED, NOT_FULLY_DELETED }

interface RegionCatalog {
    fun listRegions(): List<Region>

    fun regionContaining(
        latitude: Double,
        longitude: Double,
    ): Region?

    /** Resolves a region by id, synthesizing one from the BRouter tile grid if [id] isn't already
     *  in [listRegions] — lets downloads target a computed tile the user hasn't fetched before. */
    fun regionById(id: String): Region?

    /** Real bytes currently on disk for [region]'s tiles (0 if none are downloaded yet),
     *  independent of [Region.approximateSizeBytes]'s manifest estimate. */
    fun actualSizeOnDiskBytes(region: Region): Long

    /** Deletes [region]'s tile files from disk. [RegionDeletionResult.FULLY_DELETED] only if every
     *  tile file was either already absent or successfully deleted; [RegionDeletionResult.NOT_FULLY_DELETED]
     *  if at least one could not be removed (the caller is expected to skip and move on rather than
     *  treat this as fatal, per FR-012). */
    fun deleteRegion(region: Region): RegionDeletionResult
}
