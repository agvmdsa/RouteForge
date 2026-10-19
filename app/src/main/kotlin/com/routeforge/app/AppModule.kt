package com.routeforge.app

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        viewModel { IncomingItemsViewModel(resolveSharedText = get(), applicationContext = androidContext()) }
    }
