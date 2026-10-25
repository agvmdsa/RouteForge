package com.routeforge.mocklocationsetup.data.di

import com.routeforge.mocklocationsetup.data.PlatformMockLocationSetupStateSource
import com.routeforge.mocklocationsetup.domain.MockLocationSetupStateSource
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val mockLocationSetupDataModule =
    module {
        singleOf(::PlatformMockLocationSetupStateSource).bind<MockLocationSetupStateSource>()
    }
