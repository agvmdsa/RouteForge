package com.routeforge.designsystem.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.routeforge.designsystem.components.ConfirmationBottomSheet

private val ControlsRowSpacing = 8.dp

/** Shared add-waypoint confirmation, used identically on Plan Route and Simulate's waypoint-edit
 *  surfaces (FR-006 of spec 008) — takes pre-formatted strings rather than owning its own string
 *  resources, matching [ConfirmationBottomSheet]'s existing no-owned-resources pattern. */
@Composable
fun AddWaypointConfirmationSheet(
    title: String,
    coordinatesMessage: String,
    saveAsFavoriteLabel: String,
    favoriteNameLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    isSaveAsFavoriteChecked: Boolean,
    favoriteNameInput: String,
    onToggleSaveAsFavorite: () -> Unit,
    onFavoriteNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationBottomSheet(title = title, onDismiss = onDismiss) {
        Text(coordinatesMessage)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isSaveAsFavoriteChecked, onCheckedChange = { onToggleSaveAsFavorite() })
            Text(saveAsFavoriteLabel)
        }
        if (isSaveAsFavoriteChecked) {
            OutlinedTextField(
                value = favoriteNameInput,
                onValueChange = onFavoriteNameChange,
                label = { Text(favoriteNameLabel) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
            Button(onClick = onConfirm, enabled = !isSaveAsFavoriteChecked || favoriteNameInput.isNotBlank()) {
                Text(confirmLabel)
            }
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        }
    }
}
