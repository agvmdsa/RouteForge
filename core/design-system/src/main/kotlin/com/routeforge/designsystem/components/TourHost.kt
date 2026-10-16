package com.routeforge.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeforge.coredomain.TourProgressRepository
import com.routeforge.coredomain.TourReplayCoordinator
import com.routeforge.coredomain.model.TourSection
import com.routeforge.coredomain.tour.TourEngine
import com.routeforge.coredomain.tour.TourPhase
import com.routeforge.coredomain.tour.TourPosition
import com.routeforge.coredomain.tour.TourStepStatus
import kotlinx.coroutines.delay

// A control that appears as a consequence of the user's last action (the cancel "X" right after a
// mock is confirmed) registers its bounds in the layout pass *after* the state change, and that
// registration reaches this host a frame later still. Judging "is this step's control on screen?"
// any sooner would wrongly skip the step.
private const val SETTLE_FRAMES = 2

// Long enough to read one short line before the first step takes over the screen.
private const val CONTINUE_MESSAGE_MILLIS = 2_200L

/** The tour for one screen ([section]): replaces the screen's own content [Box], so its overlays
 *  share the content area's coordinate space with the screen's banners and controls. Provides the
 *  target registry [Modifier.tourTarget] reports into, owns where the tour is, starts it on a first
 *  visit or a Settings replay, and shows each phase — see `TourEngine` for how steps advance.
 *
 *  What shows depends on the phase (all steps only ever move forward):
 *  - waiting on an action step: [TourActionHint] plus a [TourInstructionStrip] (hidden while the
 *    step [TourStep.isPaused]), drawn in-screen with no dimming and no touch blocking;
 *  - presenting an explanatory step, or showing an action's result callout: a full-screen modal
 *    window ([TourModalWindow]) that blocks every touch and screen-reader reach beneath it, the
 *    bottom tab bar included.
 *
 *  Skip anywhere ends the whole section. Finishing or skipping marks [section] seen and reports it to
 *  [replayCoordinator] so Settings' "Repeat all" can move on — except that a run where no step ever
 *  applied (so the user saw nothing) leaves [section] unseen, to run later once there is something to
 *  point at. The tour never changes the state it observes — it only reads it through [steps].
 *  [isTopMessageVisible] is true while the screen shows one of its own transient top messages, so the
 *  strip sits beneath it (FR-020). */
@Composable
fun TourHost(
    section: TourSection,
    steps: List<TourStep>,
    progressRepository: TourProgressRepository,
    replayCoordinator: TourReplayCoordinator,
    labels: TourLabels,
    isTopMessageVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val registry = remember { TourTargetRegistry() }
    var contentOriginOnScreen by remember { mutableStateOf(Offset.Zero) }
    var position by remember { mutableStateOf<TourPosition?>(null) }
    var hasPresentedAnything by remember { mutableStateOf(false) }
    var continueMessage by remember { mutableStateOf<String?>(null) }

    val seenSections by progressRepository.observeSeenSections().collectAsStateWithLifecycle()
    val pendingReplay by replayCoordinator.pendingReplay.collectAsStateWithLifecycle()

    fun start() {
        hasPresentedAnything = false
        position = TourEngine.Start
    }

    // [markSeen] is false only when nothing at all applied to show: a section the user never got to
    // see must stay unseen so its tour still runs once there is something for it to point at.
    // Settings' "Repeat all" is told either way.
    fun finish(markSeen: Boolean = true) {
        if (markSeen) progressRepository.markSeen(section)
        replayCoordinator.notifyReplayFinished(section)
        position = null
    }

    fun skip() = finish(markSeen = true)

    LaunchedEffect(seenSections) {
        if (section !in seenSections && position == null) start()
    }
    LaunchedEffect(pendingReplay) {
        if (pendingReplay == section) {
            val continuesSequence = replayCoordinator.consumeReplay(section)
            // Already running (the first-visit start got there first): restarting would only
            // flash the first step again.
            if (position == null) {
                val greeting = labels.continueMessage.takeIf { continuesSequence }
                if (greeting == null) start() else continueMessage = greeting
            }
        }
    }

    // Its own effect rather than a delay inside the one above: consuming the request clears
    // [pendingReplay], which would cancel that effect — and the greeting with it — right away.
    continueMessage?.let { greeting ->
        LaunchedEffect(greeting) {
            delay(CONTINUE_MESSAGE_MILLIS)
            continueMessage = null
            start()
        }
    }

    Box(
        modifier =
            modifier.onGloballyPositioned {
                val origin = it.positionOnScreen()
                if (origin.isSpecified) contentOriginOnScreen = origin
            },
    ) {
        CompositionLocalProvider(LocalTourTargetRegistry provides registry) { content() }

        continueMessage?.let { greeting ->
            ConfirmationPill(
                message = greeting,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = tourTopOffset(isTopMessageVisible))
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        val current = position ?: return@Box
        val statuses = steps.map { it.statusIn(registry) }

        LaunchedEffect(statuses, current) {
            repeat(SETTLE_FRAMES) { withFrameNanos { } }
            val settled = TourEngine.settle(statuses, current)
            if (settled == null) {
                finish(markSeen = hasPresentedAnything)
            } else {
                hasPresentedAnything = true
                if (settled != current) position = settled
            }
        }

        fun next() {
            val settled = TourEngine.settle(statuses, TourEngine.onNext(current))
            if (settled == null) skip() else position = settled
        }

        val step = steps.getOrNull(current.index) ?: return@Box
        when (current.phase) {
            TourPhase.ARRIVING -> Unit
            TourPhase.WAITING -> ActionStepHint(step, registry, labels, isTopMessageVisible, onSkip = ::skip)
            TourPhase.PRESENTING, TourPhase.RESULT -> {
                val isLastStep = statuses.drop(current.index + 1).none { it.isApplicable }
                TourModalWindow(onDismissRequest = ::skip) { windowOriginOnScreen, windowSize ->
                    if (current.phase == TourPhase.PRESENTING) {
                        val bounds = step.targetId?.let(registry::boundsFor)
                        if (bounds != null) {
                            SpotlightOverlay(
                                text = step.text,
                                targetBoundsOnScreen = bounds,
                                windowOriginOnScreen = windowOriginOnScreen,
                                windowSize = windowSize,
                                isLastStep = isLastStep,
                                labels = labels,
                                onNext = ::next,
                                onSkip = ::skip,
                            )
                        }
                    } else {
                        ResultCallout(
                            text = step.result?.text.orEmpty(),
                            contentOriginOnScreen = contentOriginOnScreen,
                            windowOriginOnScreen = windowOriginOnScreen,
                            windowSize = windowSize,
                            isLastStep = isLastStep,
                            labels = labels,
                            isTopMessageVisible = isTopMessageVisible,
                            onNext = ::next,
                            onSkip = ::skip,
                        )
                    }
                }
            }
        }
    }
}

private fun TourStep.statusIn(registry: TourTargetRegistry) =
    TourStepStatus(
        kind = kind,
        isApplicable = isApplicable && (targetId == null || registry.isTargetAvailable(targetId)),
        isSatisfied = isSatisfied,
        resultOutcome = result?.outcome,
    )

@Composable
private fun ActionStepHint(
    step: TourStep,
    registry: TourTargetRegistry,
    labels: TourLabels,
    isTopMessageVisible: Boolean,
    onSkip: () -> Unit,
) {
    val targetBounds = step.targetId?.let(registry::boundsFor)
    // The control the hint points at has just left the screen; the host moves on in a moment.
    if (step.targetId != null && targetBounds == null) return

    TourActionHint(targetBoundsOnScreen = targetBounds)
    if (!step.isPaused) {
        TourInstructionStrip(
            text = step.text,
            skipLabel = labels.skip,
            onSkip = onSkip,
            isTopMessageVisible = isTopMessageVisible,
        )
    }
}

/** The callout after an action step, shown in the modal window but with no scrim so the changed
 *  path stays visible. It takes the strip's place — the content area's top, [Spacing.BannerTopOffset]
 *  down — which in this window means offsetting by where the content area starts within it. */
@Composable
private fun ResultCallout(
    text: String,
    contentOriginOnScreen: Offset,
    windowOriginOnScreen: Offset,
    windowSize: IntSize,
    isLastStep: Boolean,
    labels: TourLabels,
    isTopMessageVisible: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    if (windowSize == IntSize.Zero) return
    val contentTop = with(LocalDensity.current) { (contentOriginOnScreen.y - windowOriginOnScreen.y).coerceAtLeast(0f).toDp() }
    TourInstructionStrip(
        text = text,
        skipLabel = labels.skip,
        onSkip = onSkip,
        modifier = Modifier.padding(top = contentTop),
        isTopMessageVisible = isTopMessageVisible,
        nextLabel = if (isLastStep) labels.done else labels.next,
        onNext = onNext,
    )
}
