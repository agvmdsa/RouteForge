package com.routeforge.designsystem.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.routeforge.coredomain.model.RequiredRegionsSummary
import com.routeforge.designsystem.components.ConfirmationBottomSheet

private val ControlsRowSpacing = 8.dp

/** Shared "missing map data" warning, used wherever a route computation needs offline regions
 *  that aren't fully downloaded yet (Plan Route, Saved Routes export, and spec 008's waypoint-edit
 *  surfaces on both Plan Route and Simulate) — takes pre-formatted strings rather than owning its
 *  own string resources, matching [ConfirmationBottomSheet]'s existing no-owned-resources pattern.
 *  [summary] is used only for [RequiredRegionsSummary.uncoveredWaypointCount]'s branch, not text. */
@Composable
fun MissingRegionsWarningSheet(
    summary: RequiredRegionsSummary,
    title: String,
    message: String,
    uncoveredNote: String?,
    goToDownloadsLabel: String,
    continueAnywayLabel: String,
    onGoToDownloads: () -> Unit,
    onContinueAnyway: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationBottomSheet(title = title, onDismiss = onDismiss) {
        Text(message)
        if (summary.uncoveredWaypointCount > 0 && uncoveredNote != null) {
            Text(text = uncoveredNote, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing)) {
            Button(onClick = onGoToDownloads) { Text(goToDownloadsLabel) }
            TextButton(onClick = onContinueAnyway) { Text(continueAnywayLabel) }
        }
    }
}
