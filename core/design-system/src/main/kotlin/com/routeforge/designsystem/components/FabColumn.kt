package com.routeforge.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.routeforge.designsystem.theme.Spacing

/** A column of controls (typically FABs) anchored to one corner/edge of a map screen's [Box] —
 *  the shared shape behind every "bottom-start"/"bottom-end" FAB stack in the app, which used to
 *  be hand-rolled identically in up to 4 places differing only in [alignment]. */
@Composable
fun BoxScope.FabColumn(
    alignment: Alignment,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.ControlsRow),
        modifier = modifier.align(alignment).padding(Spacing.ScreenContent),
        content = content,
    )
}
