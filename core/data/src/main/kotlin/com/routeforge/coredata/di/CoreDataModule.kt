package com.routeforge.coredata.di

import com.routeforge.coredata.PlatformMockLocationAuthorizationChecker
import com.routeforge.coredomain.DraftWaypointsHolder
import com.routeforge.coredomain.LastComputedRouteHolder
import com.routeforge.coredomain.LastKnownRealLocationHolder
import com.routeforge.coredomain.MockLocationAuthorizationChecker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreDataModule =
    module {
        singleOf(::PlatformMockLocationAuthorizationChecker).bind<MockLocationAuthorizationChecker>()
        single { LastComputedRouteHolder() }
        single { LastKnownRealLocationHolder() }
        single { DraftWaypointsHolder() }
    }
