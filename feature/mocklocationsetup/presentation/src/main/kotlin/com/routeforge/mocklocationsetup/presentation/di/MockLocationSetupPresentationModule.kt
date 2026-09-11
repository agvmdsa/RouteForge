package com.routeforge.mocklocationsetup.presentation.di

import com.routeforge.mocklocationsetup.domain.usecase.ObserveSetupStateUseCase
import com.routeforge.mocklocationsetup.presentation.MockLocationSetupViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val mockLocationSetupPresentationModule =
    module {
        factoryOf(::ObserveSetupStateUseCase)
        viewModelOf(::MockLocationSetupViewModel)
    }
