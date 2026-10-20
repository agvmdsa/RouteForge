package com.routeforge.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.routeforge.coredomain.TourProgressRepository
import com.routeforge.coredomain.model.TourSection
import com.routeforge.coredomain.tour.TourResultOutcome
import com.routeforge.coredomain.tour.TourStepKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

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

/** One stop in a section's tour. An [TourStepKind.EXPLANATORY] step dims the screen and spotlights
 *  [targetId] (required) with [text]; an [TourStepKind.ACTION] step waits for the user to do
 *  something real and advances by itself once [isSatisfied]. [targetId] must match a
 *  [Modifier.tourTarget] id registered on the same screen — or in the app shell around it, like the
 *  bottom tab bar ([TourTargets]); for an action step it is the control the hint rings, or `null`
 *  for the center of the screen.
 *
 *  [isApplicable] is the screen-state rule for whether the step applies at all — [TourHost] also
 *  requires the target to be on screen. [isPaused] (action steps) hides the instruction strip while
 *  the confirmation the action leads to is open. [result] (action steps) is the callout shown once
 *  the action is done. */
data class TourStep(
    val id: String,
    val text: String,
    val kind: TourStepKind = TourStepKind.EXPLANATORY,
    val targetId: String? = null,
    val isApplicable: Boolean = true,
    val isSatisfied: Boolean = false,
    val isPaused: Boolean = false,
    val result: TourStepResult? = null,
) {
    init {
        require(kind == TourStepKind.ACTION || targetId != null) { "Explanatory step '$id' needs a target to spotlight" }
    }
}

data class TourStepResult(
    val text: String,
    val outcome: TourResultOutcome,
)

/** The words a tour needs, passed in because `core:design-system` has no resources. [continueMessage]
 *  is what a section greets the user with when it starts right after the previous one in "Repeat
 *  all"; `null` for a section that never follows another. */
data class TourLabels(
    val skip: String,
    val next: String,
    val done: String,
    val continueMessage: String? = null,
)

/** Targets that live in the app shell around every screen rather than in a screen's own content —
 *  they register in the app-wide registry (see [LocalTourTargetRegistry]) and any screen's tour can
 *  point at them. */
object TourTargets {
    /** The Saved tab in the bottom bar. */
    const val SavedTab = "app_tab_saved"
}

/** Holds each registered target's current on-screen bounds for a single screen's tour — not
 *  shared across screens, so [TourHost] creates one per screen. A target leaves the registry when
 *  its control leaves the composition, so a control that has since disappeared is never offered
 *  as a step's target. Lookups fall back to [parent], the app-wide registry the shell's own
 *  controls (the bottom bar) report into. */
class TourTargetRegistry(
    private val parent: TourTargetRegistry? = null,
) {
    private val boundsById = mutableStateMapOf<String, Rect>()

    internal fun register(
        id: String,
        bounds: Rect,
    ) {
        boundsById[id] = bounds
    }

    internal fun unregister(id: String) {
        boundsById.remove(id)
    }

    internal fun boundsFor(id: String): Rect? = boundsById[id] ?: parent?.boundsFor(id)

    /** Whether [id]'s control is currently on screen — without exposing [Rect] itself outside this
     *  module. */
    fun isTargetAvailable(id: String): Boolean = boundsById.containsKey(id) || parent?.isTargetAvailable(id) == true
}

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
 *  through every nested composable between a screen's root (where [TourHost] is) and the
 *  deeply-nested controls it points at (e.g. Simulation's controls live across
 *  `RouteProgressCard`/`PlaybackButton`/`WaypointEditSection`/`CoordinatePill`, none of which
 *  otherwise have any reason to know a tour exists). `null` by default — `tourTarget` is then a
 *  no-op, so existing composables are unaffected on any screen that doesn't provide one. The app
 *  provides one for the whole shell, which [TourHost] uses as the parent of its own. */
val LocalTourTargetRegistry = compositionLocalOf<TourTargetRegistry?> { null }

/** Reports this composable's current on-screen bounds into the ambient [LocalTourTargetRegistry]
 *  under [id] (a no-op if none is provided), so [TourHost] can find and point at it, and removes
 *  them when this composable leaves the composition. Bounds are in *screen* coordinates, the one
 *  space shared by this screen's window and the separate full-screen window [TourModalWindow]
 *  draws in — window coordinates would be relative to each window's own origin and shift the
 *  cutout by the status-bar height. Pure layout observation — doesn't change anything about the
 *  wrapped content's own behavior. */
fun Modifier.tourTarget(id: String): Modifier =
    composed {
        val registry = LocalTourTargetRegistry.current
        if (registry == null) {
            this
        } else {
            DisposableEffect(registry, id) { onDispose { registry.unregister(id) } }
            this.onGloballyPositioned { coordinates ->
                val position = coordinates.positionOnScreen()
                if (position.isSpecified) {
                    registry.register(id, Rect(position, coordinates.size.toSize()))
                }
            }
        }
    }

/** Removes the platform's own dim behind the enclosing dialog window — the tour draws its own scrim
 *  (or none at all for the result callout), so a system dim would darken the screen regardless.
 *  Deliberately does not touch the window's size: with `usePlatformDefaultWidth = false` +
 *  `decorFitsSystemWindows = false` and fill-max-size content, Compose's own dialog layout already
 *  goes full-screen and sets `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS`, but only while the window height
 *  is still `WRAP_CONTENT` — forcing `MATCH_PARENT` here first would skip that. */
@Composable
private fun DisableDialogWindowDim() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(0f) }
}

private val TourDialogProperties =
    DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
    )

/** A full-screen modal window for the parts of a tour that must block everything else: the
 *  spotlight on an explanatory step and the result callout.
 *
 *  A separate window rather than a layer inside the hosting screen's own layout: the app's bottom
 *  tab bar belongs to an outer `Scaffold`, so an in-screen layer could neither dim nor block it. A
 *  modal window covers the entire display (status bar and tab bar included), swallows every touch
 *  outside its own content, and keeps screen-reader focus inside it. System Back counts as
 *  [onDismissRequest] (Skip); tapping outside does nothing. One window serves both phases so it
 *  doesn't flicker out between a step and its result.
 *
 *  [content] gets this window's origin on screen and its size: targets are registered in screen
 *  coordinates ([Modifier.tourTarget]) and have to be translated into this window's own space. */
@Composable
internal fun TourModalWindow(
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.(windowOriginOnScreen: Offset, windowSize: IntSize) -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = TourDialogProperties) {
        DisableDialogWindowDim()
        var windowOriginOnScreen by remember { mutableStateOf(Offset.Zero) }
        var windowSize by remember { mutableStateOf(IntSize.Zero) }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        val position = it.positionOnScreen()
                        if (position.isSpecified) windowOriginOnScreen = position
                        windowSize = it.size
                    },
        ) {
            content(windowOriginOnScreen, windowSize)
        }
    }
}

/** Dims the whole window, cuts a rounded-rectangle hole around [targetBoundsOnScreen], and shows an
 *  explanatory bubble next to it with Skip/Next (Done on the [isLastStep]) actions. Meant to be the
 *  content of a [TourModalWindow].
 *
 *  The scrim is drawn into its own compositing layer (`graphicsLayer` forcing offscreen compositing)
 *  so `BlendMode.Clear` can actually punch transparency through it; without that, `BlendMode.Clear`
 *  has nothing to clear against and the cutout wouldn't appear. Takes its button labels as plain
 *  strings (not an owned string resource) — `core:design-system` has no `res/` of its own; every
 *  component here takes text from its caller, same as [TooltipAnchor]/[StatusPill].
 *
 *  The bubble clamps horizontally to [ScreenEdgeMargin] from either window edge, and flips above
 *  the target instead of below it when there isn't enough room underneath (e.g. a bottom-row
 *  control near the screen's bottom edge) — both measured against the window's own size, which
 *  also varies by device/orientation. */
@Composable
internal fun SpotlightOverlay(
    text: String,
    targetBoundsOnScreen: Rect,
    windowOriginOnScreen: Offset,
    windowSize: IntSize,
    isLastStep: Boolean,
    labels: TourLabels,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val density = LocalDensity.current
    var bubbleSize by remember(text) { mutableStateOf(IntSize.Zero) }
    val targetBounds = targetBoundsOnScreen.translate(-windowOriginOnScreen)

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
            val maxLeft = (windowSize.width - bubbleSize.width - marginPx).coerceAtLeast(marginPx)
            val clampedLeft = targetBounds.left.coerceIn(marginPx, maxLeft)
            val spaceBelow = windowSize.height - targetBounds.bottom - gapPx
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
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onSkip) { Text(labels.skip) }
                TextButton(onClick = onNext) { Text(if (isLastStep) labels.done else labels.next) }
            }
        }
    }
}
