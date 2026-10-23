package com.routeforge.app

import com.routeforge.addresssearch.data.di.addressSearchDataModule
import com.routeforge.addresssearch.presentation.di.addressSearchPresentationModule
import com.routeforge.coredata.di.coreDataModule
import com.routeforge.mocklocationsetup.data.di.mockLocationSetupDataModule
import com.routeforge.mocklocationsetup.presentation.di.mockLocationSetupPresentationModule
import com.routeforge.routing.data.di.routingDataModule
import com.routeforge.routing.presentation.di.routingPresentationModule
import com.routeforge.simulation.data.di.simulationDataModule
import com.routeforge.simulation.presentation.di.simulationPresentationModule
import org.koin.core.module.Module

internal val routeForgeModules: List<Module> =
    listOf(
        appModule,
        coreDataModule,
        addressSearchDataModule,
        addressSearchPresentationModule,
        mockLocationSetupDataModule,
        mockLocationSetupPresentationModule,
        routingDataModule,
        routingPresentationModule,
        simulationDataModule,
        simulationPresentationModule,
    )
