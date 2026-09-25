package com.routeforge.mocklocationsetup.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.mockLocationSetupGraph(onSetupReady: () -> Unit) {
    composable<MockLocationSetupRoute> {
        MockLocationSetupRoot(onSetupReady = onSetupReady)
    }
}
