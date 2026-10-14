package com.routeforge.designsystem.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Wraps [content] (an existing `IconButton`/`FloatingActionButton`, unchanged) so that
 *  long-pressing it shows [tooltipText] — `TooltipBox`'s own `enableUserInput` (default `true`)
 *  already handles the long-press/hover/keyboard-focus gesture internally, so this adds no custom
 *  gesture code and can't conflict with a sibling drag gesture the way a hand-rolled
 *  `pointerInput(onLongPress = ...)` could have (confirmed against the real Material3 source, not
 *  assumed). One shared implementation instead of repeating `TooltipBox`/`PlainTooltip`/
 *  `rememberTooltipState()` at every call site. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipAnchor(
    tooltipText: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(tooltipText) } },
        state = rememberTooltipState(),
        modifier = modifier,
        content = content,
    )
}
