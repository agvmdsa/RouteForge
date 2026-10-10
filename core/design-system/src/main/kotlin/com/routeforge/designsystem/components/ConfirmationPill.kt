package com.routeforge.designsystem.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val IconTextSpacing = 8.dp

/** A brief, one-line success confirmation (e.g. "Waypoints updated") — a checkmark plus [message],
 *  built on [StatusPill] so it reads as the same family of pill as this app's other status pills.
 *  The caller owns timing — there is no built-in auto-dismiss here. */
@Composable
fun ConfirmationPill(
    message: String,
    modifier: Modifier = Modifier,
) {
    StatusPill(modifier = modifier) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(IconTextSpacing))
        Text(message)
    }
}
