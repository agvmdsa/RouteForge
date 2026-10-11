package com.routeforge.simulation.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.routeforge.coredomain.model.RoutePlaybackMode
import com.routeforge.designsystem.components.TwoButtonConfirmationSheet
import com.routeforge.simulation.presentation.R

@Composable
internal fun TeleportConfirmationSheet(
    latitude: Double,
    longitude: Double,
    isBlockedOffline: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TwoButtonConfirmationSheet(
        title = stringResource(R.string.simulation_teleport_confirm_title),
        message = stringResource(R.string.simulation_teleport_confirm_message, latitude, longitude),
        confirmLabel = stringResource(R.string.simulation_teleport_confirm_button),
        dismissLabel = stringResource(R.string.simulation_teleport_cancel_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmEnabled = !isBlockedOffline,
        extraContent = {
            if (isBlockedOffline) {
                Text(
                    text = stringResource(R.string.simulation_teleport_blocked_offline_message),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
internal fun CancelMockConfirmationSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TwoButtonConfirmationSheet(
        title = stringResource(R.string.simulation_cancel_mock_confirm_title),
        message = stringResource(R.string.simulation_cancel_mock_confirm_message),
        confirmLabel = stringResource(R.string.simulation_cancel_mock_confirm_button),
        dismissLabel = stringResource(R.string.simulation_cancel_mock_dismiss_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun JoystickInterruptSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TwoButtonConfirmationSheet(
        title = stringResource(R.string.simulation_joystick_interrupt_title),
        message = stringResource(R.string.simulation_joystick_interrupt_message),
        confirmLabel = stringResource(R.string.simulation_joystick_interrupt_confirm),
        dismissLabel = stringResource(R.string.simulation_joystick_interrupt_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun CancelRouteConfirmationSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TwoButtonConfirmationSheet(
        title = stringResource(R.string.simulation_cancel_route_confirm_title),
        message = stringResource(R.string.simulation_cancel_route_confirm_message),
        confirmLabel = stringResource(R.string.simulation_cancel_route_confirm_button),
        dismissLabel = stringResource(R.string.simulation_cancel_route_dismiss_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun FavoriteTeleportCancelRouteSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TwoButtonConfirmationSheet(
        title = stringResource(R.string.simulation_favorite_teleport_cancel_route_title),
        message = stringResource(R.string.simulation_favorite_teleport_cancel_route_message),
        confirmLabel = stringResource(R.string.simulation_favorite_teleport_cancel_route_confirm),
        dismissLabel = stringResource(R.string.simulation_favorite_teleport_cancel_route_dismiss),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun SwitchRouteModeSheet(
    targetMode: RoutePlaybackMode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title =
        when (targetMode) {
            RoutePlaybackMode.GUIDED -> stringResource(R.string.simulation_switch_mode_to_guided_title)
            RoutePlaybackMode.FREE_ROAM -> stringResource(R.string.simulation_switch_mode_to_free_roam_title)
        }
    TwoButtonConfirmationSheet(
        title = title,
        message = stringResource(R.string.simulation_switch_mode_message),
        confirmLabel = stringResource(R.string.simulation_switch_mode_confirm),
        dismissLabel = stringResource(R.string.simulation_switch_mode_dismiss),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
