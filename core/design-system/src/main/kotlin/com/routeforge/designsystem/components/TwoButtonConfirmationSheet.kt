package com.routeforge.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.routeforge.designsystem.theme.Spacing

/** The shared shape behind every "are you sure" sheet with exactly two actions — an optional
 *  [message], an optional bit of [extraContent] (e.g. a warning line), and a confirm/dismiss
 *  button pair — built on [ConfirmationBottomSheet]. Used to be hand-rolled per feature. */
@Composable
fun TwoButtonConfirmationSheet(
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    confirmEnabled: Boolean = true,
    extraContent: @Composable () -> Unit = {},
) {
    ConfirmationBottomSheet(title = title, onDismiss = onDismiss) {
        message?.let { Text(it) }
        extraContent()
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.ControlsRow)) {
            Button(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        }
    }
}
