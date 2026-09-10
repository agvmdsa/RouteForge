package com.routeforge.simulation.presentation.di

import com.routeforge.simulation.presentation.SimulationViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val simulationPresentationModule =
    module {
        viewModelOf(::SimulationViewModel)
    }
