package com.routeforge.designsystem.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val IconTextSpacing = 8.dp
private const val DEFAULT_AUTO_DISMISS_DURATION_MILLIS = 2_500L

/** A brief, one-line success confirmation (e.g. "Waypoints updated") — a checkmark plus [message],
 *  built on [StatusPill] so it reads as the same family of pill as this app's other status pills.
 *  The caller owns timing — there is no built-in auto-dismiss here. */
@Composable
internal fun ConfirmationPill(
    message: String,
    modifier: Modifier = Modifier,
) {
    StatusPill(modifier = modifier) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(IconTextSpacing))
        Text(message)
    }
}

/** A [ConfirmationPill] that shows while [isVisible], then calls [onDismiss] on its own after
 *  [durationMillis] — the shared shape behind every transient "X updated" success toast in the
 *  app, which used to be hand-rolled per screen (including its own copy of the duration). */
@Composable
fun AutoDismissConfirmationPill(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Long = DEFAULT_AUTO_DISMISS_DURATION_MILLIS,
) {
    if (!isVisible) return
    LaunchedEffect(Unit) {
        delay(durationMillis)
        onDismiss()
    }
    ConfirmationPill(message = message, modifier = modifier)
}
