package com.routeforge.routing.presentation.di

import com.routeforge.coredomain.usecase.ComputeEditableWaypointRangeUseCase
import com.routeforge.coredomain.usecase.ComputeRequiredRegionsUseCase
import com.routeforge.coredomain.usecase.ComputeRouteUseCase
import com.routeforge.coredomain.usecase.FreeRoamRouteBuilder
import com.routeforge.coredomain.usecase.ObserveMapStyleUseCase
import com.routeforge.coredomain.usecase.PrepareRouteOptionsUseCase
import com.routeforge.coredomain.usecase.RecomputeRouteForBothModesUseCase
import com.routeforge.coredomain.usecase.RecordRegionUsageUseCase
import com.routeforge.coredomain.usecase.SetMapStyleUseCase
import com.routeforge.routing.domain.usecase.ComputeRegionUsageUseCase
import com.routeforge.routing.domain.usecase.DeleteRegionUseCase
import com.routeforge.routing.domain.usecase.DownloadRegionUseCase
import com.routeforge.routing.domain.usecase.EnforceStorageQuotaUseCase
import com.routeforge.routing.domain.usecase.GetStorageUsageSummaryUseCase
import com.routeforge.routing.domain.usecase.ObserveRegionCatalogUseCase
import com.routeforge.routing.domain.usecase.ObserveStorageQuotaUseCase
import com.routeforge.routing.domain.usecase.SetStorageQuotaUseCase
import com.routeforge.routing.presentation.favorites.FavoritesViewModel
import com.routeforge.routing.presentation.regioncatalog.RegionCatalogViewModel
import com.routeforge.routing.presentation.regioncoveragemap.RegionCoverageMapViewModel
import com.routeforge.routing.presentation.routerequest.IncomingRouteReceiver
import com.routeforge.routing.presentation.routerequest.PendingWaypointConfirmer
import com.routeforge.routing.presentation.routerequest.RouteComputer
import com.routeforge.routing.presentation.routerequest.RouteDraftEditor
import com.routeforge.routing.presentation.routerequest.RouteModePreviewer
import com.routeforge.routing.presentation.routerequest.RouteRequestViewModel
import com.routeforge.routing.presentation.routerequest.waypointedit.WaypointEditViewModel
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
        factoryOf(::RecomputeRouteForBothModesUseCase)
        factoryOf(::ComputeEditableWaypointRangeUseCase)
        factoryOf(::ObserveMapStyleUseCase)
        factoryOf(::SetMapStyleUseCase)
        factory {
            RouteModePreviewer(
                prepareRouteOptions = get(),
                computeRequiredRegions = get(),
                lastComputedRouteHolder = get(),
            )
        }
        factory {
            RouteComputer(
                prepareRouteOptions = get(),
                computeRequiredRegions = get(),
                lastComputedRouteHolder = get(),
            )
        }
        factory {
            RouteDraftEditor(
                draftWaypointsHolder = get(),
                selectedFavoriteWaypointHolder = get(),
                routeModePreviewer = get(),
            )
        }
        factory {
            PendingWaypointConfirmer(
                favoriteWaypointsRepository = get(),
                pendingSearchWaypointHolder = get(),
                routeDraftEditor = get(),
            )
        }
        factory {
            IncomingRouteReceiver(
                importRouteFile = get(),
                pendingIncomingRouteHolder = get(),
                routeDraftEditor = get(),
            )
        }
        viewModel {
            RouteRequestViewModel(
                routeModePreviewer = get(),
                routeComputer = get(),
                routeDraftEditor = get(),
                pendingWaypointConfirmer = get(),
                incomingRouteReceiver = get(),
                importRouteFile = get(),
                exportRouteFile = get(),
                lastKnownRealLocationHolder = get(),
                draftWaypointsHolder = get(),
                favoriteRoutesRepository = get(),
                observeMapStyle = get(),
            )
        }
        viewModelOf(::WaypointEditViewModel)
        viewModelOf(::RegionCatalogViewModel)
        viewModelOf(::RegionCoverageMapViewModel)
        viewModelOf(::SettingsViewModel)
        viewModel {
            SavedRoutesViewModel(
                favoriteRoutesRepository = get(),
                lastComputedRouteHolder = get(),
                computeRequiredRegions = get(),
                prepareRouteOptions = get(),
                exportRouteFile = get(),
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
