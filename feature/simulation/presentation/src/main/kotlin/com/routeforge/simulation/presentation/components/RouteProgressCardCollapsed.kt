package com.routeforge.simulation.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.routeforge.designsystem.components.TooltipAnchor
import com.routeforge.designsystem.components.tourTarget
import com.routeforge.designsystem.theme.RouteForgeTheme
import com.routeforge.designsystem.theme.Spacing
import com.routeforge.simulation.presentation.R
import com.routeforge.simulation.presentation.SimulationAction
import com.routeforge.simulation.presentation.SimulationState

private val ProgressCardCollapsedHorizontalPadding = 16.dp
private val ProgressCardCollapsedVerticalPadding = 8.dp

@Composable
internal fun RouteProgressCardCollapsed(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
    percent: Int,
    etaLabel: String,
    onExpand: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.ControlsRow),
        modifier =
            Modifier.padding(
                start = ProgressCardCollapsedHorizontalPadding,
                end = Spacing.ControlsRow,
                top = ProgressCardCollapsedVerticalPadding,
                bottom = ProgressCardCollapsedVerticalPadding,
            ),
    ) {
        Text(
            text = stringResource(R.string.simulation_route_progress_collapsed_label, percent, etaLabel),
            style = MaterialTheme.typography.bodyMedium,
        )
        PlaybackButton(
            state = state,
            onAction = onAction,
            modifier = Modifier.size(ControlIconButtonSize).tourTarget("sim_playback"),
        )
        val expandDescription = stringResource(R.string.simulation_route_progress_expand)
        TooltipAnchor(tooltipText = expandDescription) {
            IconButton(
                onClick = onExpand,
                modifier = Modifier.size(ToggleIconButtonSize).tourTarget("sim_expand_toggle"),
            ) {
                Icon(Icons.Filled.ExpandMore, contentDescription = expandDescription)
            }
        }
    }
}

@Preview
@Composable
private fun RouteProgressCardCollapsedPreview() {
    RouteForgeTheme {
        Surface {
            RouteProgressCardCollapsed(
                state = SimulationState(),
                onAction = {},
                percent = 40,
                etaLabel = "10 min",
                onExpand = {},
            )
        }
    }
}
