package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.DraftWaypointsHolder
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.RegionUsageTracker
import com.routeforge.routing.domain.StorageQuotaStore
import com.routeforge.routing.domain.model.RegionStatus

/** Silently removes least-recently-used downloaded regions until total usage is at or under the
 *  configured budget (FR-006), never touching a region needed by the current route draft or the
 *  active/loaded route (FR-007), stopping once only protected regions remain (FR-008), and
 *  skipping — rather than aborting on — any region that fails to delete (FR-012). A no-op when the
 *  budget is Unlimited. */
class EnforceStorageQuotaUseCase(
    private val storageQuotaStore: StorageQuotaStore,
    private val regionCatalog: RegionCatalog,
    private val regionUsageTracker: RegionUsageTracker,
    private val draftWaypointsHolder: DraftWaypointsHolder,
    private val lastComputedRouteHolder: LastComputedRouteHolder,
) {
    operator fun invoke() {
        val quotaBytes = storageQuotaStore.observeMaxBytes().value ?: return

        val downloadedRegions =
            regionCatalog.listRegions().filter {
                it.status == RegionStatus.DOWNLOADED || it.status == RegionStatus.PARTIALLY_DOWNLOADED
            }
        var totalBytes = downloadedRegions.sumOf { regionCatalog.actualSizeOnDiskBytes(it) }
        if (totalBytes <= quotaBytes) return

        val protectedIds = protectedRegionIds()
        val candidates =
            downloadedRegions
                .filter { it.id !in protectedIds }
                .sortedBy { regionUsageTracker.lastUsedAt(it.id) ?: 0L }

        for (region in candidates) {
            if (totalBytes <= quotaBytes) break
            val freedBytes = regionCatalog.actualSizeOnDiskBytes(region)
            if (regionCatalog.deleteRegion(region)) {
                regionUsageTracker.clear(region.id)
                totalBytes -= freedBytes
            }
        }
    }

    private fun protectedRegionIds(): Set<String> {
        val draftPoints = draftWaypointsHolder.points.value
        val activeRoutePoints = lastComputedRouteHolder.route.value?.points.orEmpty()
        return (draftPoints + activeRoutePoints)
            .mapNotNull { point -> regionCatalog.regionContaining(point.latitude, point.longitude)?.id }
            .toSet()
    }
}
