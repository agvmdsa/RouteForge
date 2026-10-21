package com.routeforge.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.max

private val CenterCircleRadius = 44.dp
private val TargetCirclePadding = 12.dp
private val RingWidth = 4.dp
private val RingOutlineWidth = 8.dp
private val ArrowLength = 44.dp
private val ArrowHeadWidth = 30.dp
private val ArrowHeadLength = 20.dp
private val ArrowShaftWidth = 10.dp
private val ArrowOutlineWidth = 3.dp
private val ArrowGapFromCircle = 10.dp
private val ArrowBobDistance = 12.dp
private const val PULSE_MILLIS = 900
private const val MIN_PULSE_SCALE = 0.92f
private const val MAX_PULSE_SCALE = 1.08f
private const val STATIC_PHASE = 0.5f
private const val CIRCLE_FILL_ALPHA = 0.18f

/** The no-dimming "do this here" highlight an action step shows: a pulsing circle with a bobbing
 *  arrow pointing at it. Fills its parent, draws only, and has no pointer input — touches fall
 *  through to whatever is underneath, since the user has to perform the very action it points at.
 *
 *  With [targetBoundsOnScreen] `null` the circle sits at the center of this composable's own area
 *  (a fixed spot that deliberately corresponds to no real map coordinate, FR-005); otherwise it
 *  rings that control, the arrow on whichever side has more room (FR-006). Bounds are in screen
 *  coordinates, so this composable subtracts its own screen position before drawing.
 *
 *  Each shape is drawn twice — the theme's primary over a white outline — so it stays readable on
 *  every map style (FR-019). With reduced motion (system "remove animations") it is drawn static.
 *  Decorative: no semantics of its own, so a screen reader doesn't announce it separately. */
@Composable
fun TourActionHint(
    modifier: Modifier = Modifier,
    targetBoundsOnScreen: Rect? = null,
) {
    val phase: State<Float> =
        if (rememberReducedMotion()) {
            rememberUpdatedState(STATIC_PHASE)
        } else {
            rememberInfiniteTransition(label = "tourActionHint").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(PULSE_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "tourActionHintPhase",
            )
        }
    var originOnScreen by remember { mutableStateOf(Offset.Zero) }
    val accent = MaterialTheme.colorScheme.primary

    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    val position = it.positionOnScreen()
                    if (position.isSpecified) originOnScreen = position
                }.clearAndSetSemantics {},
    ) {
        val target = targetBoundsOnScreen?.translate(-originOnScreen)
        val center = target?.center ?: Offset(size.width / 2f, size.height / 2f)
        val baseRadius =
            if (target != null) {
                max(target.width, target.height) / 2f + TargetCirclePadding.toPx()
            } else {
                CenterCircleRadius.toPx()
            }
        val radius = baseRadius * lerp(MIN_PULSE_SCALE, MAX_PULSE_SCALE, phase.value)

        drawCircle(color = accent.copy(alpha = CIRCLE_FILL_ALPHA), radius = radius, center = center)
        drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = RingOutlineWidth.toPx()))
        drawCircle(color = accent, radius = radius, center = center, style = Stroke(width = RingWidth.toPx()))

        val gap = ArrowGapFromCircle.toPx()
        val bob = ArrowBobDistance.toPx()
        val arrowLength = ArrowLength.toPx()
        val spaceBelow = size.height - (center.y + radius)
        val spaceAbove = center.y - radius
        val arrowBelow = spaceBelow >= gap + bob + arrowLength || spaceBelow >= spaceAbove
        val direction = if (arrowBelow) 1f else -1f

        val tip = Offset(center.x, center.y + direction * (radius + gap + bob * phase.value))
        val headBaseY = tip.y + direction * ArrowHeadLength.toPx()
        val tailY = tip.y + direction * arrowLength
        val halfHead = ArrowHeadWidth.toPx() / 2f
        val halfShaft = ArrowShaftWidth.toPx() / 2f
        val arrow =
            Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x + halfHead, headBaseY)
                lineTo(tip.x + halfShaft, headBaseY)
                lineTo(tip.x + halfShaft, tailY)
                lineTo(tip.x - halfShaft, tailY)
                lineTo(tip.x - halfShaft, headBaseY)
                lineTo(tip.x - halfHead, headBaseY)
                close()
            }
        drawPath(path = arrow, color = accent)
        drawPath(path = arrow, color = Color.White, style = Stroke(width = ArrowOutlineWidth.toPx(), join = StrokeJoin.Round))
    }
}
