package com.routeforge.coredomain.handoff

import com.routeforge.coredomain.model.RoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hand-off of the route-creation screen's current waypoints, kept here so other screens (for example the region catalog) can
 * tell what map data is actually needed without a direct feature-to-feature dependency. The latest value is kept and any
 * number of readers observe it.
 *
 * Carries the draft's `List<RoutePoint>`, empty at the start. Writers: `RouteDraftEditor`, `RouteFileTransfer`. Readers:
 * `ComputeRegionUsageUseCase`, `EnforceStorageQuotaUseCase`, `RegionCatalogViewModel`. Latest value kept.
 */
interface DraftWaypointsHandoffWriter {
    fun set(points: List<RoutePoint>)
}

interface DraftWaypointsHandoffReader {
    val points: StateFlow<List<RoutePoint>>
}

class DraftWaypointsHandoffImpl :
    DraftWaypointsHandoffWriter,
    DraftWaypointsHandoffReader {
    private val draft = MutableStateFlow<List<RoutePoint>>(emptyList())

    override val points: StateFlow<List<RoutePoint>> = draft.asStateFlow()

    override fun set(points: List<RoutePoint>) {
        draft.value = points
    }
}
