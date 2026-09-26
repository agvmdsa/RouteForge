package com.routeforge.simulation.presentation

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.routeforge.designsystem.components.StatusPill
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationSession

@Composable
internal fun CoordinatePill(
    mockedSession: SimulationSession?,
    isSearchingRealLocation: Boolean,
    hasKnownRealLocation: Boolean,
    onCancelMockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val text =
        when {
            mockedSession != null -> "%.5f, %.5f".format(mockedSession.latitude, mockedSession.longitude)
            isSearchingRealLocation && !hasKnownRealLocation -> stringResource(R.string.simulation_status_searching_real_location)
            else -> stringResource(R.string.simulation_status_no_simulation)
        }
    val copyLabel = stringResource(R.string.simulation_copy_coordinates)
    StatusPill(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                if (mockedSession != null) {
                    Modifier.clickable(onClickLabel = copyLabel) { clipboardManager.setText(AnnotatedString(text)) }
                } else {
                    Modifier
                },
        )
        if (mockedSession?.mode == SimulationMode.STATIONARY) {
            IconButton(onClick = onCancelMockClick) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.simulation_cancel_mock_button))
            }
        }
    }
}
