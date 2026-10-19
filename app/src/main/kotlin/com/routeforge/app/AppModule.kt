package com.routeforge.app

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule =
    module {
        viewModelOf(::IncomingItemsViewModel)
    }
