package com.routeforge.designsystem.waypointedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.routeforge.coredomain.model.ModeAvailability

private val SheetContentPadding = 16.dp
private val ControlsRowSpacing = 8.dp
private const val DEFAULT_SHEET_HEIGHT_FRACTION = 0.5f
/** A fixed estimate, not measured — reserved in [listHeight] up front, the instant
 *  [hasUnappliedReorder] turns true, rather than discovered reactively via [onSizeChanged] after
 *  the fact. Measuring it reactively doesn't work here: on the very first frame the row appears,
 *  the list has already been laid out assuming zero footer height (since it had never been shown
 *  before), consuming the sheet's entire fixed height and leaving the footer no room to ever
 *  render — a one-way deadlock, not just a one-frame flicker. */
private val ApplyChangesRowHeight = 72.dp

/** Shared edit sheet (FR-002) hosting [WaypointReorderList] over the map — opens at roughly half
 *  the available height by default (user-draggable larger, up to full), with the map remaining
 *  visible and interactive in the space above it since this composable only occupies the bottom
 *  sheet, not the whole screen. Shows [inactiveModeUnavailableMessage] when [inactiveModeAvailability]
 *  is [ModeAvailability.UNAVAILABLE_PENDING_RETRY] (FR-011/SC-004). Reordering only updates the
 *  displayed order instantly (no lag mid-drag) — [hasUnappliedReorder] drives an explicit
 *  "Update changes"/"Cancel" row, since the actual recompute is deferred to [onApplyChanges] rather
 *  than running on every intermediate swap. That row (and the title) are kept outside the list's
 *  own scroll area, so they stay visible no matter how long the list gets — only the waypoint list
 *  itself scrolls. */
@Composable
fun WaypointEditSheet(
    title: String,
    items: List<WaypointListItem>,
    inactiveModeAvailability: ModeAvailability,
    inactiveModeUnavailableMessage: String,
    hasUnappliedReorder: Boolean,
    applyChangesLabel: String,
    cancelChangesLabel: String,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onDelete: (id: String) -> Unit,
    onApplyChanges: () -> Unit,
    onCancelChanges: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val sheetHeight = maxHeight * DEFAULT_SHEET_HEIGHT_FRACTION
            var headerHeightPx by remember { mutableIntStateOf(0) }
            val headerHeight = with(density) { headerHeightPx.toDp() }
            val footerHeight = if (hasUnappliedReorder) ApplyChangesRowHeight else 0.dp
            val listHeight = (sheetHeight - headerHeight - footerHeight).coerceAtLeast(0.dp)

            Column(modifier = Modifier.height(sheetHeight)) {
                Column(modifier = Modifier.onSizeChanged { headerHeightPx = it.height }) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(SheetContentPadding),
                    )
                    if (inactiveModeAvailability == ModeAvailability.UNAVAILABLE_PENDING_RETRY) {
                        Text(
                            text = inactiveModeUnavailableMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = SheetContentPadding),
                        )
                    }
                }
                WaypointReorderList(
                    items = items,
                    onReorder = onReorder,
                    onDelete = onDelete,
                    modifier = Modifier.fillMaxWidth().height(listHeight),
                )
                if (hasUnappliedReorder) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ControlsRowSpacing),
                        modifier = Modifier.fillMaxWidth().height(ApplyChangesRowHeight).padding(SheetContentPadding),
                    ) {
                        Button(onClick = onApplyChanges) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Text(applyChangesLabel)
                        }
                        TextButton(onClick = onCancelChanges) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                            Text(cancelChangesLabel)
                        }
                    }
                }
            }
        }
    }
}
