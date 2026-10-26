package com.routeforge.coredata.di

import com.routeforge.coredata.AndroidNetworkConnectivityChecker
import com.routeforge.coredata.HttpClientFactory
import com.routeforge.coredata.HttpRedirectSource
import com.routeforge.coredata.PlatformMockLocationAuthorizationChecker
import com.routeforge.coredata.SharedPreferencesFavoriteRoutesRepository
import com.routeforge.coredata.SharedPreferencesFavoriteWaypointsRepository
import com.routeforge.coredata.SharedPreferencesMapStyleStore
import com.routeforge.coredata.SharedPreferencesTourProgressRepository
import com.routeforge.coredomain.FavoriteRoutesRepository
import com.routeforge.coredomain.FavoriteWaypointsRepository
import com.routeforge.coredomain.MapStyleStore
import com.routeforge.coredomain.MockLocationAuthorizationChecker
import com.routeforge.coredomain.NetworkConnectivityChecker
import com.routeforge.coredomain.TourProgressRepository
import com.routeforge.coredomain.TourReplayCoordinator
import com.routeforge.coredomain.handoff.DraftWaypointsHandoffImpl
import com.routeforge.coredomain.handoff.DraftWaypointsHandoffReader
import com.routeforge.coredomain.handoff.DraftWaypointsHandoffWriter
import com.routeforge.coredomain.handoff.LastKnownRealLocationHandoffImpl
import com.routeforge.coredomain.handoff.LastKnownRealLocationHandoffReader
import com.routeforge.coredomain.handoff.LastKnownRealLocationHandoffWriter
import com.routeforge.coredomain.handoff.PendingIncomingRouteHandoffImpl
import com.routeforge.coredomain.handoff.PendingIncomingRouteHandoffReceiver
import com.routeforge.coredomain.handoff.PendingIncomingRouteHandoffSender
import com.routeforge.coredomain.handoff.PendingSearchWaypointHandoffImpl
import com.routeforge.coredomain.handoff.PendingSearchWaypointHandoffReceiver
import com.routeforge.coredomain.handoff.PendingSearchWaypointHandoffSender
import com.routeforge.coredomain.handoff.PendingTeleportTargetHandoffImpl
import com.routeforge.coredomain.handoff.PendingTeleportTargetHandoffReceiver
import com.routeforge.coredomain.handoff.PendingTeleportTargetHandoffSender
import com.routeforge.coredomain.handoff.SelectedFavoriteWaypointHandoffImpl
import com.routeforge.coredomain.handoff.SelectedFavoriteWaypointHandoffReceiver
import com.routeforge.coredomain.handoff.SelectedFavoriteWaypointHandoffSender
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.link.RedirectFollower
import com.routeforge.coredomain.link.RedirectSource
import com.routeforge.coredomain.link.ResolveSharedTextUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val coreDataModule =
    module {
        single { HttpClientFactory.create() }
        singleOf(::PlatformMockLocationAuthorizationChecker).bind<MockLocationAuthorizationChecker>()
        single { LastComputedRouteHolder() }
        single<LastKnownRealLocationHandoffWriter> {
            LastKnownRealLocationHandoffImpl()
        } binds arrayOf(LastKnownRealLocationHandoffReader::class)
        single<DraftWaypointsHandoffWriter> {
            DraftWaypointsHandoffImpl()
        } binds arrayOf(DraftWaypointsHandoffReader::class)
        single<PendingTeleportTargetHandoffSender> {
            PendingTeleportTargetHandoffImpl()
        } binds arrayOf(PendingTeleportTargetHandoffReceiver::class)
        single<PendingSearchWaypointHandoffSender> {
            PendingSearchWaypointHandoffImpl()
        } binds arrayOf(PendingSearchWaypointHandoffReceiver::class)
        single<PendingIncomingRouteHandoffSender> {
            PendingIncomingRouteHandoffImpl()
        } binds arrayOf(PendingIncomingRouteHandoffReceiver::class)
        single<SelectedFavoriteWaypointHandoffSender> {
            SelectedFavoriteWaypointHandoffImpl()
        } binds arrayOf(SelectedFavoriteWaypointHandoffReceiver::class)
        single { SharedPreferencesFavoriteWaypointsRepository(androidContext()) }.bind<FavoriteWaypointsRepository>()
        single { SharedPreferencesFavoriteRoutesRepository(androidContext()) }.bind<FavoriteRoutesRepository>()
        single { AndroidNetworkConnectivityChecker(androidContext()) }.bind<NetworkConnectivityChecker>()
        single { SharedPreferencesMapStyleStore(androidContext()) }.bind<MapStyleStore>()
        single { SharedPreferencesTourProgressRepository(androidContext(), get()) }.bind<TourProgressRepository>()
        single { TourReplayCoordinator() }
        single { HttpRedirectSource(get()) }.bind<RedirectSource>()
        single { RedirectFollower(get()) }
        factoryOf(::ResolveSharedTextUseCase)
    }
