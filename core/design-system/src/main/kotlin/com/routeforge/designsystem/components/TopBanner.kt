package com.routeforge.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BannerShape = RoundedCornerShape(12.dp)
private val BannerContentPadding = 12.dp

/** An error/warning surface meant to float over a map or other full-bleed content. Positioning
 *  (alignment, top offset, side padding) is the caller's call — pass it in via [modifier] — this
 *  component only owns the shape, color, and internal padding. */
@Composable
fun TopBanner(
    modifier: Modifier = Modifier,
    contentPadding: Dp = BannerContentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = BannerShape,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
