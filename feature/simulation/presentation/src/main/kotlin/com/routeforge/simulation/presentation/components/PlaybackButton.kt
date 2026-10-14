package com.routeforge.simulation.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routeforge.designsystem.components.TooltipAnchor
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationStatus
import com.routeforge.simulation.presentation.R
import com.routeforge.simulation.presentation.SimulationAction
import com.routeforge.simulation.presentation.SimulationState

private val PlaybackButtonSize = 48.dp

@Composable
internal fun PlaybackButton(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
    modifier: Modifier = Modifier.size(PlaybackButtonSize),
) {
    val session = state.mockedSession
    val (icon, description, action) =
        if (session?.mode == SimulationMode.ROUTE && session.status == SimulationStatus.RUNNING) {
            Triple(Icons.Filled.Pause, stringResource(R.string.simulation_pause_button), SimulationAction.OnPauseSimulation)
        } else if (session?.mode == SimulationMode.ROUTE && session.status == SimulationStatus.PAUSED) {
            Triple(Icons.Filled.PlayArrow, stringResource(R.string.simulation_resume_button), SimulationAction.OnResumeSimulation)
        } else {
            Triple(Icons.Filled.PlayArrow, stringResource(R.string.simulation_start_button), SimulationAction.OnStartRouteSimulation)
        }
    TooltipAnchor(tooltipText = description) {
        IconButton(onClick = { onAction(action) }, modifier = modifier) {
            Icon(icon, contentDescription = description)
        }
    }
}
