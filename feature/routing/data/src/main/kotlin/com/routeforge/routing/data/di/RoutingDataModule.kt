package com.routeforge.routing.data.di

import com.routeforge.coredata.HttpClientFactory
import com.routeforge.routing.data.BrouterRoutingEngine
import com.routeforge.routing.data.BundledRegionCatalog
import com.routeforge.routing.data.GpxRouteFileCodec
import com.routeforge.routing.data.HttpRegionDownloader
import com.routeforge.routing.data.JsonRouteFileCodec
import com.routeforge.routing.domain.RegionCatalog
import com.routeforge.routing.domain.RegionDownloader
import com.routeforge.routing.domain.RouteFileCodec
import com.routeforge.routing.domain.RoutingEngine
import com.routeforge.routing.domain.model.RouteFileFormat
import com.routeforge.routing.domain.usecase.ExportRouteFileUseCase
import com.routeforge.routing.domain.usecase.ImportRouteFileUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

val routingDataModule =
    module {
        single { HttpClientFactory.create() }

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

        single<Map<RouteFileFormat, RouteFileCodec>> {
            mapOf(
                RouteFileFormat.JSON to JsonRouteFileCodec(),
                RouteFileFormat.GPX to GpxRouteFileCodec(),
            )
        }
        single { ImportRouteFileUseCase(get()) }
        single { ExportRouteFileUseCase(get()) }
    }

