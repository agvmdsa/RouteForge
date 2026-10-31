package com.routeforge.designsystem.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

private const val ADDRESS_MAX_LINES = 3

/**
 * What a confirmation says about a position. With an [address] it is the main line (at most three lines, cut with an ellipsis) and
 * the [coordinates] sit below it in smaller text; without one it is the [coordinates] alone, exactly as before the address existed.
 */
@Composable
fun PlaceDescription(
    address: String?,
    coordinates: String,
    modifier: Modifier = Modifier,
) {
    if (address == null) {
        Text(text = coordinates, modifier = modifier)
    } else {
        Column(modifier = modifier) {
            Text(
                text = address,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = ADDRESS_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = coordinates,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
