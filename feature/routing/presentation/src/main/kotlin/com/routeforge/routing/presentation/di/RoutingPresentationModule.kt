package com.routeforge.routing.presentation.di

import com.routeforge.routing.domain.FreeRoamRouteBuilder
import com.routeforge.routing.domain.usecase.ComputeRegionUsageUseCase
import com.routeforge.routing.domain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.routing.domain.usecase.ComputeRouteUseCase
import com.routeforge.routing.domain.usecase.DeleteRegionUseCase
import com.routeforge.routing.domain.usecase.DownloadRegionUseCase
import com.routeforge.routing.domain.usecase.EnforceStorageQuotaUseCase
import com.routeforge.routing.domain.usecase.GetStorageUsageSummaryUseCase
import com.routeforge.routing.domain.usecase.ObserveRegionCatalogUseCase
import com.routeforge.routing.domain.usecase.ObserveStorageQuotaUseCase
import com.routeforge.routing.domain.usecase.PrepareRouteOptionsUseCase
import com.routeforge.routing.domain.usecase.RecordRegionUsageUseCase
import com.routeforge.routing.domain.usecase.SetStorageQuotaUseCase
import com.routeforge.routing.presentation.favorites.FavoritesViewModel
import com.routeforge.routing.presentation.regioncatalog.RegionCatalogViewModel
import com.routeforge.routing.presentation.regioncoveragemap.RegionCoverageMapViewModel
import com.routeforge.routing.presentation.routerequest.RouteRequestViewModel
import com.routeforge.routing.presentation.savedroutes.SavedRoutesViewModel
import com.routeforge.routing.presentation.settings.SettingsViewModel
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
        factoryOf(::DeleteRegionUseCase)
        factoryOf(::ComputeRegionUsageUseCase)
        viewModel {
            RouteRequestViewModel(
                prepareRouteOptions = get(),
                lastComputedRouteHolder = get(),
                importRouteFile = get(),
                exportRouteFile = get(),
                lastKnownRealLocationHolder = get(),
                computeRequiredRegions = get(),
                draftWaypointsHolder = get(),
                favoriteWaypointsRepository = get(),
                favoriteRoutesRepository = get(),
                selectedFavoriteWaypointHolder = get(),
            )
        }
        viewModelOf(::RegionCatalogViewModel)
        viewModelOf(::RegionCoverageMapViewModel)
        viewModelOf(::SettingsViewModel)
        viewModel {
            SavedRoutesViewModel(
                favoriteRoutesRepository = get(),
                lastComputedRouteHolder = get(),
                computeRequiredRegions = get(),
                prepareRouteOptions = get(),
            )
        }
        viewModel { (isPickerMode: Boolean) ->
            FavoritesViewModel(
                isPickerMode = isPickerMode,
                favoriteWaypointsRepository = get(),
                pendingTeleportTargetHolder = get(),
                selectedFavoriteWaypointHolder = get(),
                networkConnectivityChecker = get(),
            )
        }
    }

