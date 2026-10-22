package com.routeforge.simulation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.simulation.presentation.components.CancelMockConfirmationSheet
import com.routeforge.simulation.presentation.components.CancelRouteConfirmationSheet
import com.routeforge.simulation.presentation.components.FavoriteTeleportCancelRouteSheet
import com.routeforge.simulation.presentation.components.JoystickInterruptSheet
import com.routeforge.simulation.presentation.components.StartRouteDialog
import com.routeforge.simulation.presentation.components.SwitchRouteModeSheet
import com.routeforge.simulation.presentation.components.TeleportConfirmationSheet

/** The screen's modal sheets/dialogs — each gated on its own nullable/boolean state field, shown
 *  at most one at a time in practice since they correspond to mutually-exclusive user flows. */
@Composable
internal fun SimulationDialogs(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
) {
    val pendingLatitude = state.pendingTeleportLatitude
    val pendingLongitude = state.pendingTeleportLongitude
    if (pendingLatitude != null && pendingLongitude != null) {
        TeleportConfirmationSheet(
            latitude = pendingLatitude,
            longitude = pendingLongitude,
            isBlockedOffline = state.isPendingTeleportBlockedOffline,
            onConfirm = { onAction(SimulationAction.OnConfirmTeleport) },
            onDismiss = { onAction(SimulationAction.OnCancelTeleport) },
        )
    }

    if (state.isPendingCancelMock) {
        CancelMockConfirmationSheet(
            onConfirm = { onAction(SimulationAction.OnConfirmCancelMock) },
            onDismiss = { onAction(SimulationAction.OnDismissCancelMock) },
        )
    }

    if (state.pendingFavoriteTeleportTarget != null) {
        FavoriteTeleportCancelRouteSheet(
            onConfirm = { onAction(SimulationAction.OnConfirmFavoriteTeleportCancelRoute) },
            onDismiss = { onAction(SimulationAction.OnDismissFavoriteTeleportCancelRoute) },
        )
    }

    val pendingRouteModeSwitch = state.pendingRouteModeSwitch
    if (pendingRouteModeSwitch != null) {
        SwitchRouteModeSheet(
            targetMode = pendingRouteModeSwitch,
            onConfirm = { onAction(SimulationAction.OnConfirmSwitchRouteMode) },
            onDismiss = { onAction(SimulationAction.OnDismissSwitchRouteMode) },
        )
    }

    if (state.isJoystickInterruptPending) {
        JoystickInterruptSheet(
            onConfirm = { onAction(SimulationAction.OnConfirmJoystickInterrupt) },
            onDismiss = { onAction(SimulationAction.OnDismissJoystickInterrupt) },
        )
    }

    if (state.isPendingCancelRoute) {
        CancelRouteConfirmationSheet(
            onConfirm = { onAction(SimulationAction.OnStopSimulation) },
            onDismiss = { onAction(SimulationAction.OnDismissCancelRoute) },
        )
    }

    if (state.isStartRouteDialogOpen) {
        StartRouteDialog(
            state = state,
            onAction = onAction,
        )
    }
}

@Preview
@Composable
private fun SimulationDialogsPreview() {
    RouteForgeTheme {
        SimulationDialogs(state = SimulationState(isPendingCancelMock = true), onAction = {})
    }
}
