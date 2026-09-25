package com.routeforge.simulation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routeforge.designsystem.components.ConfirmationBottomSheet

private val ControlsRowSpacing = 8.dp

@Composable
internal fun TeleportConfirmationSheet(
    latitude: Double,
    longitude: Double,
    isBlockedOffline: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationBottomSheet(title = stringResource(R.string.simulation_teleport_confirm_title), onDismiss = onDismiss) {
        Text(stringResource(R.string.simulation_teleport_confirm_message, latitude, longitude))
        if (isBlockedOffline) {
            Text(
                text = stringResource(R.string.simulation_teleport_blocked_offline_message),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
            Button(onClick = onConfirm, enabled = !isBlockedOffline) {
                Text(stringResource(R.string.simulation_teleport_confirm_button))
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.simulation_teleport_cancel_button)) }
        }
    }
}

@Composable
internal fun CancelMockConfirmationSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationBottomSheet(title = stringResource(R.string.simulation_cancel_mock_confirm_title), onDismiss = onDismiss) {
        Text(stringResource(R.string.simulation_cancel_mock_confirm_message))
        Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
            Button(onClick = onConfirm) { Text(stringResource(R.string.simulation_cancel_mock_confirm_button)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.simulation_cancel_mock_dismiss_button)) }
        }
    }
}

@Composable
internal fun JoystickInterruptSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationBottomSheet(title = stringResource(R.string.simulation_joystick_interrupt_title), onDismiss = onDismiss) {
        Text(stringResource(R.string.simulation_joystick_interrupt_message))
        Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
            Button(onClick = onConfirm) { Text(stringResource(R.string.simulation_joystick_interrupt_confirm)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.simulation_joystick_interrupt_cancel)) }
        }
    }
}
