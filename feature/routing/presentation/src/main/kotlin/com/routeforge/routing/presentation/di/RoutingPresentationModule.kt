package com.routeforge.routing.presentation.di

import com.routeforge.routing.domain.FreeRoamRouteBuilder
import com.routeforge.routing.domain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.routing.domain.usecase.ComputeRouteUseCase
import com.routeforge.routing.domain.usecase.DownloadRegionUseCase
import com.routeforge.routing.domain.usecase.EnforceStorageQuotaUseCase
import com.routeforge.routing.domain.usecase.GetStorageUsageSummaryUseCase
import com.routeforge.routing.domain.usecase.ObserveRegionCatalogUseCase
import com.routeforge.routing.domain.usecase.ObserveStorageQuotaUseCase
import com.routeforge.routing.domain.usecase.PrepareRouteOptionsUseCase
import com.routeforge.routing.domain.usecase.RecordRegionUsageUseCase
import com.routeforge.routing.domain.usecase.SetStorageQuotaUseCase
import com.routeforge.routing.presentation.RegionCatalogViewModel
import com.routeforge.routing.presentation.RouteRequestViewModel
import com.routeforge.routing.presentation.SettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val routingPresentationModule =
    module {
        factoryOf(::ComputeRouteUseCase)
        factoryOf(::FreeRoamRouteBuilder)
        factoryOf(::PrepareRouteOptionsUseCase)
        factoryOf(::DownloadRegionUseCase)
        factoryOf(::ObserveRegionCatalogUseCase)
        factoryOf(::ComputeRequiredRegionsUseCase)
        factoryOf(::ObserveStorageQuotaUseCase)
        factoryOf(::SetStorageQuotaUseCase)
        factoryOf(::RecordRegionUsageUseCase)
        factoryOf(::EnforceStorageQuotaUseCase)
        factoryOf(::GetStorageUsageSummaryUseCase)
        viewModel {
            RouteRequestViewModel(
                prepareRouteOptions = get(),
                lastComputedRouteHolder = get(),
                importRouteFile = get(),
                exportRouteFile = get(),
                lastKnownRealLocationHolder = get(),
                computeRequiredRegions = get(),
                draftWaypointsHolder = get(),
            )
        }
        viewModelOf(::RegionCatalogViewModel)
        viewModelOf(::SettingsViewModel)
    }

