package com.routeforge.app

import android.app.Application
import com.routeforge.mocklocationsetup.data.di.mockLocationSetupDataModule
import com.routeforge.mocklocationsetup.presentation.di.mockLocationSetupPresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RouteForgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RouteForgeApp)
            modules(
                mockLocationSetupDataModule,
                mockLocationSetupPresentationModule,
            )
        }
    }
}
