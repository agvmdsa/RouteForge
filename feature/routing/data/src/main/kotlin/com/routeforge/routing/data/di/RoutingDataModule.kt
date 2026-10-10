package com.routeforge.routing.data.di

import com.routeforge.routing.data.BrouterRoutingEngine
import com.routeforge.routing.data.BundledRegionCatalog
import com.routeforge.routing.data.GpxRouteFileCodec
import com.routeforge.routing.data.HttpRegionDownloader
import com.routeforge.routing.data.JsonRouteFileCodec
import com.routeforge.routing.data.RegionDownloadControllerImpl
import com.routeforge.routing.data.SharedPreferencesRegionUsageTracker
import com.routeforge.routing.data.SharedPreferencesStorageQuotaStore
import com.routeforge.coredomain.RegionCatalog
import com.routeforge.routing.domain.RegionDownloadController
import com.routeforge.routing.domain.RegionDownloader
import com.routeforge.coredomain.RegionUsageTracker
import com.routeforge.routing.domain.RouteFileCodec
import com.routeforge.coredomain.RoutingEngine
import com.routeforge.routing.domain.StorageQuotaStore
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.usecase.ExportRouteFileUseCase
import com.routeforge.routing.domain.usecase.ImportRouteFileUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

val routingDataModule =
    module {
        single {
            BundledRegionCatalog(
                segmentDirectory = File(androidContext().filesDir, "routing/segments"),
                profileDirectory = File(androidContext().filesDir, "routing/profile"),
                openAsset = { path -> androidContext().assets.open(path) },
            )
        }.bind<RegionCatalog>()

        single {
            BrouterRoutingEngine(
                segmentDir = get<BundledRegionCatalog>().segmentDirectory,
                profileFile = get<BundledRegionCatalog>().profileFile,
            )
        }.bind<RoutingEngine>()

        single {
            HttpRegionDownloader(
                httpClient = get(),
                regionCatalog = get<BundledRegionCatalog>(),
            )
        }.bind<RegionDownloader>()

        single { SharedPreferencesStorageQuotaStore(androidContext()) }.bind<StorageQuotaStore>()

        single { SharedPreferencesRegionUsageTracker(androidContext()) }.bind<RegionUsageTracker>()

        single {
            RegionDownloadControllerImpl(
                downloadRegion = get(),
                regionCatalog = get<BundledRegionCatalog>(),
                regionUsageTracker = get(),
                context = androidContext(),
            )
        }.bind<RegionDownloadController>()

        single<Map<RouteFileFormat, RouteFileCodec>> {
            mapOf(
                RouteFileFormat.JSON to JsonRouteFileCodec(),
                RouteFileFormat.GPX to GpxRouteFileCodec(),
            )
        }
        single { ImportRouteFileUseCase(get()) }
        single { ExportRouteFileUseCase(get()) }
    }

