package com.routeforge.simulation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

private val ControlsRowSpacing = 8.dp
private val StartDialogOptionSpacing = 12.dp
private val StartDialogOptionCornerRadius = 16.dp
private val StartDialogOptionHorizontalPadding = 16.dp
private val StartDialogOptionVerticalPadding = 12.dp

/** Asked right when the user taps Play, so "how many times should this run" is a deliberate,
 *  well-presented choice rather than a setting buried in the tuning sheet. */
@Composable
internal fun StartRouteDialog(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(SimulationAction.OnDismissStartRouteDialog) },
        title = { Text(stringResource(R.string.simulation_start_route_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(StartDialogOptionSpacing)) {
                Text(
                    text = stringResource(R.string.simulation_start_route_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ExecutionModeSelection.entries.forEach { selection ->
                    ExecutionModeOption(
                        selection = selection,
                        isSelected = state.executionModeSelection == selection,
                        onClick = { onAction(SimulationAction.OnExecutionModeSelected(selection)) },
                    )
                }
                if (state.executionModeSelection == ExecutionModeSelection.TIMES) {
                    OutlinedTextField(
                        value = state.executionTimesInput,
                        onValueChange = { onAction(SimulationAction.OnExecutionTimesInputChange(it)) },
                        label = { Text(stringResource(R.string.simulation_execution_times_input_label)) },
                        isError = state.errorType == SimulationErrorType.INVALID_EXECUTION_TIMES,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAction(SimulationAction.OnConfirmStartRoute) }) {
                Text(stringResource(R.string.simulation_start_route_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(SimulationAction.OnDismissStartRouteDialog) }) {
                Text(stringResource(R.string.simulation_start_route_dialog_cancel))
            }
        },
    )
}

@Composable
private fun ExecutionModeOption(
    selection: ExecutionModeSelection,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(StartDialogOptionCornerRadius),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing),
            modifier =
                Modifier.padding(
                    horizontal = StartDialogOptionHorizontalPadding,
                    vertical = StartDialogOptionVerticalPadding,
                ),
        ) {
            Icon(imageVector = selection.toIcon(), contentDescription = null, tint = contentColor)
            Text(
                text = selection.toLabel(),
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}

private fun ExecutionModeSelection.toIcon() =
    when (this) {
        ExecutionModeSelection.ONCE -> Icons.Filled.PlayArrow
        ExecutionModeSelection.TIMES -> Icons.Filled.Repeat
        ExecutionModeSelection.LOOP -> Icons.Filled.Loop
    }

@Composable
private fun ExecutionModeSelection.toLabel(): String =
    when (this) {
        ExecutionModeSelection.ONCE -> stringResource(R.string.simulation_execution_once)
        ExecutionModeSelection.TIMES -> stringResource(R.string.simulation_execution_times)
        ExecutionModeSelection.LOOP -> stringResource(R.string.simulation_execution_loop)
    }
