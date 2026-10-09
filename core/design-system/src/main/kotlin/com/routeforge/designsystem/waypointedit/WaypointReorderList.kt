package com.routeforge.designsystem.waypointedit

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/** One row in [WaypointReorderList] — UI-only, deliberately decoupled from either screen's own
 *  domain types (spec 008's research.md Decision 1): [id] identifies the waypoint for callbacks,
 *  [label] is the already-formatted display text, [isEditable] drives the read-only rendering for
 *  already-passed waypoints during an active run (FR-013). */
data class WaypointListItem(
    val id: String,
    val label: String,
    val isEditable: Boolean,
)

private val RowPadding = 12.dp
private val HandleSpacerWidth = 48.dp
private const val READ_ONLY_ALPHA = 0.5f

/** Shared reorderable waypoint list (FR-001) — long-press-and-drag the handle to reorder, swipe to
 *  delete. Items with [WaypointListItem.isEditable] `false` render dimmed, with no drag handle or
 *  delete affordance, and never start a drag (FR-012/FR-013). The 2-waypoint delete floor (FR-008)
 *  is the caller's responsibility, not this component's (spec 008's `/speckit-analyze` finding
 *  C5) — [onDelete] is only ever invoked for a tap/swipe the caller already decided to allow. */
@Composable
fun WaypointReorderList(
    items: List<WaypointListItem>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

    LazyColumn(modifier = modifier) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            val isDragged = index == draggedIndex
            val offsetY = if (isDragged) dragOffset.roundToInt() else 0

            val dismissState =
                rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled && item.isEditable) {
                            onDelete(item.id)
                        }
                        false // this list is re-driven by [items] itself; never let the box stay dismissed
                    },
                )

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = item.isEditable,
                enableDismissFromEndToStart = item.isEditable,
                backgroundContent = {},
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, offsetY) }
                            .zIndex(if (isDragged) 1f else 0f)
                            .padding(RowPadding)
                            .onSizeChanged { itemHeights[index] = it.height }
                            .alpha(if (item.isEditable) 1f else READ_ONLY_ALPHA),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.isEditable) {
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = null,
                            modifier =
                                Modifier.pointerInput(item.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffset = 0f
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, delta ->
                                            change.consume()
                                            val currentIndex = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                            dragOffset += delta.y
                                            val height = itemHeights[currentIndex] ?: return@detectDragGesturesAfterLongPress
                                            if (dragOffset > height / 2 && currentIndex < items.lastIndex) {
                                                onReorder(currentIndex, currentIndex + 1)
                                                draggedIndex = currentIndex + 1
                                                dragOffset -= height
                                            } else if (dragOffset < -height / 2 && currentIndex > 0) {
                                                onReorder(currentIndex, currentIndex - 1)
                                                draggedIndex = currentIndex - 1
                                                dragOffset += height
                                            }
                                        },
                                    )
                                },
                        )
                    } else {
                        Spacer(modifier = Modifier.width(HandleSpacerWidth))
                    }
                    Text(text = item.label, modifier = Modifier.padding(horizontal = RowPadding))
                }
            }

            LaunchedEffect(items) {
                // The dismiss gesture above always resets to Settled; this just guards against a
                // leftover non-Settled state if [items] changes out from under an in-progress swipe.
                if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                    dismissState.reset()
                }
            }
        }
    }
}
