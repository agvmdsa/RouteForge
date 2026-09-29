package com.routeforge.simulation.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

private val SpinnerSize = 20.dp

/** A Google Maps-style "locate me" button — always tappable, forcing a fresh real-location
 *  search regardless of whether one is already running, showing a spinner while it's in progress. */
@Composable
internal fun ForceRealLocationButton(
    isSearching: Boolean,
    onClick: () -> Unit,
) {
    SmallFloatingActionButton(onClick = onClick) {
        if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.size(SpinnerSize))
        } else {
            Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.simulation_force_real_location_button))
        }
    }
}
