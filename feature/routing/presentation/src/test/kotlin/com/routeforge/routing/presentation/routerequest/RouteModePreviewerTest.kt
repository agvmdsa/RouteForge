package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.model.LatLng
import com.routeforge.coredomain.model.Region
import com.routeforge.coredomain.model.RegionStatus
import com.routeforge.coredomain.model.Route
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.coredomain.testing.FakeRegionCatalog
import com.routeforge.coredomain.testing.FakeRegionUsageTracker
import com.routeforge.coredomain.testing.FakeRoutingEngine
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.coredomain.usecase.ComputeRouteUseCase
import com.routeforge.coredomain.usecase.FreeRoamRouteBuilder
import com.routeforge.coredomain.usecase.PrepareRouteOptionsUseCase
import com.routeforge.coredomain.usecase.RecordRegionUsageUseCase
import com.routeforge.routing.domain.model.RouteDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RouteModePreviewerTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val routingEngine = FakeRoutingEngine(snappableLatitudes = emptySet())
    private val regionCatalog = FakeRegionCatalog()
    private val lastComputedRouteHolder = LastComputedRouteHolder()
    private val previewer =
        RouteModePreviewer(
            prepareRouteOptions =
                PrepareRouteOptionsUseCase(
                    computeRoute = ComputeRouteUseCase(routingEngine, regionCatalog, RecordRegionUsageUseCase(FakeRegionUsageTracker())),
                    freeRoamRouteBuilder = FreeRoamRouteBuilder(),
                ),
            computeRequiredRegions = ComputeRequiredRegionsUseCase(regionCatalog),
            lastComputedRouteHolder = lastComputedRouteHolder,
            backgroundDispatcher = dispatcher,
        )

    private val twoPoints = RouteDraft(points = listOf(RoutePoint(1.0, 1.0), RoutePoint(2.0, 2.0)))

    private fun routeOf(
        mode: RoutePlaybackMode,
        alternateGeometry: List<LatLng>? = null,
        alternateDistanceMeters: Double? = null,
    ) = Route(
        points = twoPoints.points,
        geometry = listOf(LatLng(1.0, 1.0), LatLng(2.0, 2.0)),
        distanceMeters = 100.0,
        mode = mode,
        alternateGeometry = alternateGeometry,
        alternateDistanceMeters = alternateDistanceMeters,
    )

    private fun makeGuidedViable() {
        routingEngine.snappableLatitudes = setOf(1.0, 2.0)
        routingEngine.computePathResult = routeOf(RoutePlaybackMode.GUIDED)
    }

    private fun addUndownloadedRegionCoveringTheDraft() {
        regionCatalog.regions =
            listOf(
                Region(
                    id = "north-zone",
                    displayName = "North Zone",
                    minLatitude = 0.0,
                    minLongitude = 0.0,
                    maxLatitude = 5.0,
                    maxLongitude = 5.0,
                    tileIds = listOf("north.rd5"),
                    approximateSizeBytes = 1_000L,
                    status = RegionStatus.NOT_DOWNLOADED,
                    missingTileCount = 1,
                ),
            )
    }

    @Test
    fun `switching a route that already has its alternate swaps the mode at once and sets the holder`() {
        val alternate = listOf(LatLng(5.0, 5.0), LatLng(6.0, 6.0))
        val route = routeOf(RoutePlaybackMode.GUIDED, alternateGeometry = alternate, alternateDistanceMeters = 50.0)
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints, route = route, chosenMode = RoutePlaybackMode.GUIDED))

        previewer.switchRouteMode(state, scope)

        assertEquals(RoutePlaybackMode.FREE_ROAM, state.value.route?.mode)
        assertEquals(RoutePlaybackMode.FREE_ROAM, state.value.chosenMode)
        assertEquals(alternate, state.value.route?.geometry)
        assertEquals(50.0, state.value.route?.distanceMeters)
        assertEquals(state.value.route, lastComputedRouteHolder.route.value)
    }

    @Test
    fun `switching a route without its alternate while a region is missing sets the warning with the target mode`() {
        addUndownloadedRegionCoveringTheDraft()
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints, route = routeOf(RoutePlaybackMode.FREE_ROAM)))

        previewer.switchRouteMode(state, scope)

        assertNotNull(state.value.missingRegionsWarning)
        assertEquals(RoutePlaybackMode.GUIDED, state.value.pendingModePreviewTarget)
        assertEquals(RoutePlaybackMode.FREE_ROAM, state.value.route?.mode)
    }

    @Test
    fun `switching a route without its alternate computes the other mode and applies it`() {
        makeGuidedViable()
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints, route = routeOf(RoutePlaybackMode.FREE_ROAM)))

        previewer.switchRouteMode(state, scope)

        assertEquals(RoutePlaybackMode.GUIDED, state.value.route?.mode)
        assertEquals(RoutePlaybackMode.GUIDED, state.value.chosenMode)
        assertFalse(state.value.isComputingPreview)
        assertEquals(state.value.route, lastComputedRouteHolder.route.value)
    }

    @Test
    fun `previewing guided with no viable path shows the no-path error and stops the spinner`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))

        previewer.computeDraftPreview(twoPoints.points, state, scope)

        assertEquals(RouteRequestError.MODE_PREVIEW_NO_PATH, state.value.errorType)
        assertFalse(state.value.isComputingPreview)
        assertNull(state.value.previewMode)
    }

    @Test
    fun `previewing guided with a viable path shows the preview route`() {
        makeGuidedViable()
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))

        previewer.computeDraftPreview(twoPoints.points, state, scope)

        assertEquals(RoutePlaybackMode.GUIDED, state.value.previewMode)
        assertNotNull(state.value.previewRoute)
        assertFalse(state.value.isComputingPreview)
    }

    @Test
    fun `asking for the other mode of a computed route with no viable path shows the no-path error`() {
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints, route = routeOf(RoutePlaybackMode.FREE_ROAM)))

        previewer.attemptModePreview(RoutePlaybackMode.GUIDED, state, scope)

        assertEquals(RouteRequestError.MODE_PREVIEW_NO_PATH, state.value.errorType)
        assertFalse(state.value.isComputingPreview)
        assertEquals(RoutePlaybackMode.FREE_ROAM, state.value.route?.mode)
    }

    @Test
    fun `refreshing a guided preview with fewer than two points clears it`() {
        val oneAndOnly = RouteDraft(points = listOf(RoutePoint(1.0, 1.0)))
        val state =
            MutableStateFlow(
                RouteRequestState(
                    draft = oneAndOnly,
                    previewMode = RoutePlaybackMode.GUIDED,
                    previewRoute = routeOf(RoutePlaybackMode.GUIDED),
                ),
            )

        previewer.refreshGuidedPreview(state, scope)

        assertNull(state.value.previewMode)
        assertNull(state.value.previewRoute)
    }

    @Test
    fun `refreshing a guided preview while a region is missing asks about the region instead of computing`() {
        addUndownloadedRegionCoveringTheDraft()
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints, previewMode = RoutePlaybackMode.GUIDED))

        previewer.refreshGuidedPreview(state, scope)

        assertNotNull(state.value.missingRegionsWarning)
        assertEquals(RoutePlaybackMode.GUIDED, state.value.pendingModePreviewTarget)
    }

    @Test
    fun `switching with a guided preview showing and no route turns the preview off`() {
        val state =
            MutableStateFlow(
                RouteRequestState(
                    draft = twoPoints,
                    previewMode = RoutePlaybackMode.GUIDED,
                    previewRoute = routeOf(RoutePlaybackMode.GUIDED),
                ),
            )

        previewer.switchRouteMode(state, scope)

        assertNull(state.value.previewMode)
        assertNull(state.value.previewRoute)
    }

    @Test
    fun `switching with no route and no preview starts a guided preview`() {
        makeGuidedViable()
        val state = MutableStateFlow(RouteRequestState(draft = twoPoints))

        previewer.switchRouteMode(state, scope)

        assertEquals(RoutePlaybackMode.GUIDED, state.value.previewMode)
        assertNotNull(state.value.previewRoute)
    }
}
