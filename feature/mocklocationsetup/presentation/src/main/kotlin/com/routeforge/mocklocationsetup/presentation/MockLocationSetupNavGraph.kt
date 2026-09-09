package com.routeforge.mocklocationsetup.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.mockLocationSetupGraph(
    navController: NavController,
    onSetupReady: () -> Unit,
) {
    composable<MockLocationSetupRoute> {
        MockLocationSetupRoot(onSetupReady = onSetupReady)
    }
}
