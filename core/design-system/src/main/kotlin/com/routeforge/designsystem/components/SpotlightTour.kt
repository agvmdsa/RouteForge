package com.routeforge.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.routeforge.coredomain.TourProgressRepository
import com.routeforge.coredomain.model.TourSection
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val ScrimColor = Color.Black.copy(alpha = 0.72f)
private val CutoutPadding = 8.dp
private val CutoutCornerRadius = 16.dp
private val CutoutBorderWidth = 2.dp
private val BubbleMaxWidth = 280.dp
private val BubbleShape = RoundedCornerShape(16.dp)
private val BubblePadding = 16.dp
private val BubbleSpacing = 8.dp
private val BubbleElevation = 8.dp
private val BubbleGapFromTarget = 12.dp
private val ScreenEdgeMargin = 16.dp

/** One spotlighted control: [targetId] must match a [Modifier.tourTarget] id registered somewhere
 *  on the same screen, [text] is the explanatory copy shown next to it. */
data class TourStep(
    val targetId: String,
    val text: String,
)

/** Holds each registered target's current on-screen bounds for a single screen's tour — not
 *  shared across screens, so `remember` it once per screen (alongside that screen's own
 *  `SpotlightOverlay`/`Modifier.tourTarget` calls). */
class TourTargetRegistry {
    private val boundsById = mutableStateMapOf<String, Rect>()

    internal fun register(
        id: String,
        bounds: Rect,
    ) {
        boundsById[id] = bounds
    }

    internal fun boundsFor(id: String): Rect? = boundsById[id]

    /** Whether [id]'s control is currently on screen — lets a hosting screen filter its full step
     *  list down to only the controls actually visible right now (e.g. a route-dependent control
     *  that isn't shown until a route is loaded), without exposing [Rect] itself outside this
     *  module. */
    fun isTargetAvailable(id: String): Boolean = boundsById.containsKey(id)
}

@Composable
fun rememberTourTargetRegistry(): TourTargetRegistry = remember { TourTargetRegistry() }

/** Reports every section as already seen and ignores [markSeen] — the default for a tour-hosting
 *  screen's `tourProgressRepository` parameter, so a bare `SomeScreen(state = ..., onAction = {})`
 *  call (Compose Previews, simple tests) never needs a live Koin graph just to render a screen that
 *  happens to host a tour; callers that DO want the tour to actually trigger (the screen's own
 *  `*Root` composable) pass the real `koinInject<TourProgressRepository>()` explicitly instead. */
class NoOpTourProgressRepository : TourProgressRepository {
    private val seenSections = MutableStateFlow(TourSection.entries.toSet())

    override fun observeSeenSections(): StateFlow<Set<TourSection>> = seenSections

    override fun markSeen(section: TourSection) = Unit
}

/** Ambient registry for [Modifier.tourTarget] — avoids threading a `TourTargetRegistry` parameter
 *  through every nested composable between a screen's root (where [SpotlightOverlay] is hosted)
 *  and the deeply-nested controls it spotlights (e.g. Simulation's controls live across
 *  `RouteProgressCard`/`PlaybackButton`/`WaypointEditSection`/`CoordinatePill`, none of which
 *  otherwise have any reason to know a tour exists). `null` by default — `tourTarget` is then a
 *  no-op, so existing composables are unaffected on any screen that doesn't provide one. */
val LocalTourTargetRegistry = compositionLocalOf<TourTargetRegistry?> { null }

/** Reports this composable's current on-screen bounds into the ambient [LocalTourTargetRegistry]
 *  under [id] (a no-op if none is provided), so a [SpotlightOverlay] elsewhere on the same screen
 *  can find and highlight it. Window coordinates (not root/local) — the only coordinate space that
 *  stays comparable regardless of how much status-bar/Scaffold inset sits between this control and
 *  wherever [SpotlightOverlay] itself ends up drawing, which otherwise varies per device. Pure
 *  layout observation — doesn't change anything about the wrapped content's own behavior. */
fun Modifier.tourTarget(id: String): Modifier =
    composed {
        val registry = LocalTourTargetRegistry.current
        if (registry == null) {
            this
        } else {
            this.onGloballyPositioned { coordinates -> registry.register(id, coordinates.boundsInWindow()) }
        }
    }

/** Dims the rest of the screen, cuts a rounded-rectangle hole around [steps]'s current target
 *  (via [registry]), and shows an explanatory bubble next to it with Skip/Next actions. Renders
 *  nothing if the current step's target hasn't been registered yet (e.g. it isn't visible in the
 *  current screen state) — there is nothing to spotlight in that case. The scrim is drawn into its
 *  own compositing layer (`graphicsLayer` forcing offscreen compositing) so `BlendMode.Clear` can
 *  actually punch transparency through it; without that, `BlendMode.Clear` has nothing to clear
 *  against and the cutout wouldn't appear. Takes its button labels as plain strings (not an owned
 *  string resource) — `core:design-system` has no `res/` of its own; every component here takes
 *  text from its caller, same as [TooltipAnchor]/[StatusPill].
 *
 *  Targets are registered in window coordinates ([Modifier.tourTarget]); this overlay measures its
 *  own position in that same space ([overlayPositionInWindow]) and subtracts it before drawing, so
 *  the cutout lines up with the real control no matter how much inset sits above this Box on a given
 *  device. The bubble clamps horizontally to [ScreenEdgeMargin] from either screen edge, and flips
 *  above the target instead of below it when there isn't enough room underneath (e.g. a bottom-row
 *  control near the screen's bottom edge) — both measured against the overlay's own size, which also
 *  varies by device/orientation. */
@Composable
fun SpotlightOverlay(
    steps: List<TourStep>,
    currentStepIndex: Int,
    registry: TourTargetRegistry,
    skipButtonLabel: String,
    nextButtonLabel: String,
    doneButtonLabel: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = steps.getOrNull(currentStepIndex) ?: return
    val targetBoundsInWindow = registry.boundsFor(step.targetId) ?: return
    val density = LocalDensity.current

    var overlayPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var bubbleSize by remember(step.targetId, currentStepIndex) { mutableStateOf(IntSize.Zero) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    overlayPositionInWindow = it.positionInWindow()
                    overlaySize = it.size
                },
    ) {
        val targetBounds = targetBoundsInWindow.translate(-overlayPositionInWindow)

        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        ) {
            drawRect(color = ScrimColor)
            val cutoutPaddingPx = with(density) { CutoutPadding.toPx() }
            val cutoutCornerPx = with(density) { CutoutCornerRadius.toPx() }
            val cutoutTopLeft = targetBounds.topLeft - Offset(cutoutPaddingPx, cutoutPaddingPx)
            val cutoutSize = Size(targetBounds.width + cutoutPaddingPx * 2, targetBounds.height + cutoutPaddingPx * 2)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = cutoutTopLeft,
                size = cutoutSize,
                cornerRadius = CornerRadius(cutoutCornerPx, cutoutCornerPx),
                blendMode = BlendMode.Clear,
            )
            drawRoundRect(
                color = Color.White,
                topLeft = cutoutTopLeft,
                size = cutoutSize,
                cornerRadius = CornerRadius(cutoutCornerPx, cutoutCornerPx),
                style = Stroke(width = with(density) { CutoutBorderWidth.toPx() }),
            )
        }

        val bubbleOffset =
            with(density) {
                val gapPx = BubbleGapFromTarget.toPx()
                val marginPx = ScreenEdgeMargin.toPx()
                val maxLeft = (overlaySize.width - bubbleSize.width - marginPx).coerceAtLeast(marginPx)
                val clampedLeft = targetBounds.left.coerceIn(marginPx, maxLeft)
                val spaceBelow = overlaySize.height - targetBounds.bottom - gapPx
                val fitsBelow = bubbleSize.height == 0 || spaceBelow >= bubbleSize.height
                val top =
                    if (fitsBelow) {
                        targetBounds.bottom + gapPx
                    } else {
                        (targetBounds.top - gapPx - bubbleSize.height).coerceAtLeast(marginPx)
                    }
                IntOffset(clampedLeft.roundToInt(), top.roundToInt())
            }
        Surface(
            shape = BubbleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = BubbleElevation,
            modifier =
                Modifier
                    .offset { bubbleOffset }
                    .widthIn(max = BubbleMaxWidth)
                    .onGloballyPositioned { bubbleSize = it.size },
        ) {
            Column(
                modifier = Modifier.padding(BubblePadding),
                verticalArrangement = Arrangement.spacedBy(BubbleSpacing),
            ) {
                Text(text = step.text, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onSkip) { Text(skipButtonLabel) }
                    val isLastStep = currentStepIndex == steps.lastIndex
                    TextButton(onClick = onNext) { Text(if (isLastStep) doneButtonLabel else nextButtonLabel) }
                }
            }
        }
    }
}
