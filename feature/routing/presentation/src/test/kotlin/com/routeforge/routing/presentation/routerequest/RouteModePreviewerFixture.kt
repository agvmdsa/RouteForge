package com.routeforge.routing.presentation.routerequest

import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.testing.FakeRegionCatalog
import com.routeforge.coredomain.testing.FakeRegionUsageTracker
import com.routeforge.coredomain.testing.FakeRoutingEngine
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.coredomain.usecase.ComputeRouteUseCase
import com.routeforge.coredomain.usecase.FreeRoamRouteBuilder
import com.routeforge.coredomain.usecase.PrepareRouteOptionsUseCase
import com.routeforge.coredomain.usecase.RecordRegionUsageUseCase
import kotlinx.coroutines.CoroutineDispatcher

internal fun newRouteModePreviewer(
    routingEngine: FakeRoutingEngine,
    regionCatalog: FakeRegionCatalog,
    dispatcher: CoroutineDispatcher,
) = RouteModePreviewer(
    prepareRouteOptions =
        PrepareRouteOptionsUseCase(
            computeRoute = ComputeRouteUseCase(routingEngine, regionCatalog, RecordRegionUsageUseCase(FakeRegionUsageTracker())),
            freeRoamRouteBuilder = FreeRoamRouteBuilder(),
        ),
    computeRequiredRegions = ComputeRequiredRegionsUseCase(regionCatalog),
    lastComputedRouteHolder = LastComputedRouteHolder(),
    backgroundDispatcher = dispatcher,
)
