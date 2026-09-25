package com.routeforge.simulation.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationStatus

private val BigButtonDiameter = 72.dp

@Composable
internal fun PlaybackButton(
    state: SimulationState,
    onAction: (SimulationAction) -> Unit,
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
    FloatingActionButton(
        onClick = { onAction(action) },
        modifier = Modifier.size(BigButtonDiameter),
    ) {
        Icon(icon, contentDescription = description)
    }
}
