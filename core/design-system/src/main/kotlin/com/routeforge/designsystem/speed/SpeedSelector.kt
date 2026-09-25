package com.routeforge.designsystem.speed

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val DefaultSpeedRangeKmh = 0f..150f

/** The floating trigger for [SpeedSelectorDialog] — a small always-in-a-corner button rather than
 *  a permanently visible slider, so speed controls don't compete for space with the map. */
@Composable
fun SpeedSelectorFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Filled.Speed, contentDescription = contentDescription)
    }
}

/** A generic km/h speed slider dialog. Deliberately decoupled from any specific screen or feature —
 *  callers pass in their own resolved strings and speed value, so the same picker can back both the
 *  joystick's speed control and route-playback speed without duplicating the dialog/slider itself. */
@Composable
fun SpeedSelectorDialog(
    title: String,
    speedLabel: String,
    confirmButtonLabel: String,
    speedKmh: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    speedRangeKmh: ClosedFloatingPointRange<Float> = DefaultSpeedRangeKmh,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(confirmButtonLabel) } },
        title = { Text(title) },
        text = {
            Column {
                Text(text = speedLabel, style = MaterialTheme.typography.labelLarge)
                Slider(value = speedKmh, onValueChange = onSpeedChange, valueRange = speedRangeKmh)
            }
        },
    )
}
