package com.routeforge.mocklocationsetup.data.di

import com.routeforge.mocklocationsetup.data.PlatformDeveloperSettingsDataSource
import com.routeforge.mocklocationsetup.data.SetupDeepLinkIntentFactory
import com.routeforge.mocklocationsetup.domain.DeveloperSettingsDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val mockLocationSetupDataModule =
    module {
        single { SetupDeepLinkIntentFactory(androidContext().packageManager) }
        singleOf(::PlatformDeveloperSettingsDataSource).bind<DeveloperSettingsDataSource>()
    }
