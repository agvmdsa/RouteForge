package com.routeforge.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

/** A circular, tinted icon container used for headline/empty-state imagery (setup steps, success
 *  and blocked states, and similar single-glance status moments). */
@Composable
fun IconBadge(
    icon: ImageVector,
    size: Dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Surface(shape = CircleShape, color = containerColor, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
    }
}
