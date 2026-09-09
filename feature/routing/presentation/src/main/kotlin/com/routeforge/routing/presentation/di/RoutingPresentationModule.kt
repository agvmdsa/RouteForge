package com.routeforge.routing.presentation.di

import com.routeforge.routing.presentation.RegionCatalogViewModel
import com.routeforge.routing.presentation.RouteRequestViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val routingPresentationModule =
    module {
        viewModel { RouteRequestViewModel(computeRoute = get()) }
        viewModelOf(::RegionCatalogViewModel)
    }
