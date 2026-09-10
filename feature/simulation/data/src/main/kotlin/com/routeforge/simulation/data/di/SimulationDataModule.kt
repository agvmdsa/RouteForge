package com.routeforge.simulation.data.di

import com.routeforge.simulation.data.AndroidMockLocationPublisher
import com.routeforge.simulation.data.SimulationControllerImpl
import com.routeforge.simulation.domain.MockLocationPublisher
import com.routeforge.simulation.domain.SimulationController
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val simulationDataModule =
    module {
        single { AndroidMockLocationPublisher(androidContext()) }.bind<MockLocationPublisher>()

        single {
            SimulationControllerImpl(
                mockLocationPublisher = get(),
                mockLocationAuthorizationChecker = get(),
                context = androidContext(),
            )
        }.bind<SimulationController>()
    }
