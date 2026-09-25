package com.routeforge.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val PillShape = RoundedCornerShape(28.dp)
private val PillElevation = 4.dp
private val PillHorizontalPadding = 16.dp
private val PillVerticalPadding = 8.dp

/** A rounded, elevated surface for a single line of at-a-glance status (current coordinates, a
 *  point count, a route summary) — the caller supplies whatever [Row] content fits the moment. */
@Composable
fun StatusPill(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = PillElevation,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = PillHorizontalPadding, vertical = PillVerticalPadding),
            content = content,
        )
    }
}
