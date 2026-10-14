package com.routeforge.coredata.di

import com.routeforge.coredata.AndroidNetworkConnectivityChecker
import com.routeforge.coredata.HttpClientFactory
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
import com.routeforge.coredomain.holder.DraftWaypointsHolder
import com.routeforge.coredomain.holder.LastComputedRouteHolder
import com.routeforge.coredomain.holder.LastKnownRealLocationHolder
import com.routeforge.coredomain.holder.PendingSearchWaypointHolder
import com.routeforge.coredomain.holder.PendingTeleportTargetHolder
import com.routeforge.coredomain.holder.SelectedFavoriteWaypointHolder
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreDataModule =
    module {
        single { HttpClientFactory.create() }
        singleOf(::PlatformMockLocationAuthorizationChecker).bind<MockLocationAuthorizationChecker>()
        single { LastComputedRouteHolder() }
        single { LastKnownRealLocationHolder() }
        single { DraftWaypointsHolder() }
        single { PendingTeleportTargetHolder() }
        single { PendingSearchWaypointHolder() }
        single { SelectedFavoriteWaypointHolder() }
        single { SharedPreferencesFavoriteWaypointsRepository(androidContext()) }.bind<FavoriteWaypointsRepository>()
        single { SharedPreferencesFavoriteRoutesRepository(androidContext()) }.bind<FavoriteRoutesRepository>()
        single { AndroidNetworkConnectivityChecker(androidContext()) }.bind<NetworkConnectivityChecker>()
        single { SharedPreferencesMapStyleStore(androidContext()) }.bind<MapStyleStore>()
        single { SharedPreferencesTourProgressRepository(androidContext(), get()) }.bind<TourProgressRepository>()
        single { TourReplayCoordinator() }
    }
