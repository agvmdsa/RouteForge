package com.routeforge.simulation.presentation.di

import com.routeforge.simulation.domain.usecase.ConfirmJoystickInterruptUseCase
import com.routeforge.simulation.domain.usecase.IsNetworkConnectedUseCase
import com.routeforge.simulation.domain.usecase.ObserveMockedSessionUseCase
import com.routeforge.simulation.domain.usecase.ObserveRealLocationUseCase
import com.routeforge.simulation.domain.usecase.PauseSimulationUseCase
import com.routeforge.simulation.domain.usecase.RequestJoystickInterruptUseCase
import com.routeforge.simulation.domain.usecase.ResumeSimulationUseCase
import com.routeforge.simulation.domain.usecase.SetExecutionModeUseCase
import com.routeforge.simulation.domain.usecase.SetSpeedUseCase
import com.routeforge.simulation.domain.usecase.StartRouteSimulationUseCase
import com.routeforge.simulation.domain.usecase.StopSimulationUseCase
import com.routeforge.simulation.domain.usecase.TeleportUseCase
import com.routeforge.simulation.domain.usecase.UpdateJoystickDirectionUseCase
import com.routeforge.simulation.presentation.SimulationViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val simulationPresentationModule =
    module {
        factoryOf(::TeleportUseCase)
        factoryOf(::StartRouteSimulationUseCase)
        factoryOf(::PauseSimulationUseCase)
        factoryOf(::ResumeSimulationUseCase)
        factoryOf(::StopSimulationUseCase)
        factoryOf(::ObserveMockedSessionUseCase)
        factoryOf(::ObserveRealLocationUseCase)
        factoryOf(::IsNetworkConnectedUseCase)
        factoryOf(::SetSpeedUseCase)
        factoryOf(::SetExecutionModeUseCase)
        factoryOf(::RequestJoystickInterruptUseCase)
        factoryOf(::ConfirmJoystickInterruptUseCase)
        factoryOf(::UpdateJoystickDirectionUseCase)
        viewModelOf(::SimulationViewModel)
    }

