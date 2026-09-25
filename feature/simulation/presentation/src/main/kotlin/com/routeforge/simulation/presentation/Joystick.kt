package com.routeforge.simulation.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val JoystickDiameter = 120.dp
private val KnobDiameter = 36.dp

/** FR-022/FR-023: drag anywhere on this pad to move; bearing is measured clockwise from north.
 *  The inner knob visually tracks the drag so the current direction is always obvious. */
@Composable
internal fun Joystick(
    onDrag: (Float) -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var totalDrag by remember { mutableStateOf(Offset.Zero) }
    val maxKnobOffsetPx = with(LocalDensity.current) { ((JoystickDiameter - KnobDiameter) / 2).toPx() }
    val knobOffset =
        run {
            val distance = sqrt(totalDrag.x * totalDrag.x + totalDrag.y * totalDrag.y)
            if (distance <= maxKnobOffsetPx || distance == 0f) totalDrag else totalDrag * (maxKnobOffsetPx / distance)
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(JoystickDiameter)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            totalDrag = Offset.Zero
                            onReleased()
                        },
                        onDragCancel = {
                            totalDrag = Offset.Zero
                            onReleased()
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                        val bearing =
                            ((Math.toDegrees(atan2(totalDrag.x.toDouble(), -totalDrag.y.toDouble())) + 360.0) % 360.0).toFloat()
                        onDrag(bearing)
                    }
                },
    ) {
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                    .size(KnobDiameter)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
    }
}
