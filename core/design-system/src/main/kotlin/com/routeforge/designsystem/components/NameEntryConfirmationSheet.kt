package com.routeforge.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.routeforge.designsystem.theme.Spacing

/** The shared shape behind every "name this and save it" sheet — a single text field plus a
 *  confirm/dismiss pair where confirm is disabled until [nameInput] is non-blank — built on
 *  [ConfirmationBottomSheet]. Used to be hand-rolled identically for both saving a route and
 *  saving a favorite waypoint. */
@Composable
fun NameEntryConfirmationSheet(
    title: String,
    nameInput: String,
    onNameChange: (String) -> Unit,
    nameLabel: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationBottomSheet(title = title, onDismiss = onDismiss) {
        OutlinedTextField(
            value = nameInput,
            onValueChange = onNameChange,
            label = { Text(nameLabel) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.ControlsRow)) {
            Button(onClick = onConfirm, enabled = nameInput.isNotBlank()) { Text(confirmLabel) }
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        }
    }
}
