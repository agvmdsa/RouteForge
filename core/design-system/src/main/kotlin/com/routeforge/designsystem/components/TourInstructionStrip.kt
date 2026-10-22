package com.routeforge.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.routeforge.designsystem.theme.Spacing

private val StripShape = RoundedCornerShape(16.dp)
private val StripElevation = 6.dp
private val StripMaxWidth = 360.dp
private val StripPadding = 12.dp
private val StripSpacing = 4.dp

/** How much further down the strip sits while an app-level top message (error banner, "waypoints
 *  updated" confirmation) occupies the [Spacing.BannerTopOffset] slot. A fixed value sized for the
 *  tallest such message (the authorization banner with its button); there is no measuring of the
 *  message itself, so retune it on a device if a banner ever grows past it. */
private val TopMessageExtraOffset = 96.dp

/** Distance from the top of the content area at which the tour's strip, callout and greeting sit:
 *  the banner slot, or below it while the screen's own top message occupies that slot (FR-020). */
internal fun tourTopOffset(isTopMessageVisible: Boolean): Dp =
    if (isTopMessageVisible) Spacing.BannerTopOffset + TopMessageExtraOffset else Spacing.BannerTopOffset

/** The light instruction strip an action step shows (and the result callout reuses): [text], a Skip
 *  button, and a Next button only when [nextLabel] is given — action steps advance on their own and
 *  never offer Next (FR-002); the result callout does.
 *
 *  Sits at the top center, [Spacing.BannerTopOffset] down — plus [TopMessageExtraOffset] while
 *  [isTopMessageVisible] so it never covers, nor is covered by, the app's own top message (FR-020).
 *  The text is a polite live region so a screen reader announces it when it appears and again when
 *  it changes (FR-019). Takes label strings as parameters — `core:design-system` has no resources. */
@Composable
internal fun TourInstructionStrip(
    text: String,
    skipLabel: String,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    isTopMessageVisible: Boolean = false,
    nextLabel: String? = null,
    onNext: () -> Unit = {},
) {
    val topOffset = tourTopOffset(isTopMessageVisible)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = Spacing.ScreenContent, end = Spacing.ScreenContent, top = topOffset),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            shape = StripShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = StripElevation,
            modifier = Modifier.widthIn(max = StripMaxWidth),
        ) {
            Column(
                modifier = Modifier.padding(StripPadding),
                verticalArrangement = Arrangement.spacedBy(StripSpacing),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onSkip) { Text(skipLabel) }
                    if (nextLabel != null) {
                        TextButton(onClick = onNext) { Text(nextLabel) }
                    }
                }
            }
        }
    }
}
