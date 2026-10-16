package com.routeforge.coredomain

import com.routeforge.coredomain.model.TourSection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** Coordinates Settings' manual tour replay (spec 013 FR-009) between screens that otherwise don't
 *  know about each other: Settings requests a replay by [TourSection] via [requestReplay], the app
 *  brings that section's tab forward (Settings can't — it is already off screen by the second
 *  section of "Repeat all"), the matching screen sees the request in [pendingReplay] once it's
 *  composed, force-starts its own tour regardless of prior seen-state and clears it with
 *  [consumeReplay], then reports back via [notifyReplayFinished] so "Repeat all" (FR-008) knows when
 *  to move on to the next section.
 *
 *  A pending request is state, not a one-shot event: several screens observe it, and only the one
 *  whose section matches may take it — an event channel delivers to a single collector, so a
 *  non-matching screen could consume and drop it. State also still delivers a request made just
 *  before navigating to the target screen once that screen's composition starts observing a moment
 *  later. */
class TourReplayCoordinator {
    private val _pendingReplay = MutableStateFlow<TourSection?>(null)
    val pendingReplay: StateFlow<TourSection?> = _pendingReplay.asStateFlow()

    private val replayCompletionChannel = Channel<TourSection>(Channel.BUFFERED)
    val replayCompletions: Flow<TourSection> = replayCompletionChannel.receiveAsFlow()

    private var pendingContinuesSequence = false

    /** [continuesSequence] marks a request that follows a section the user just finished in "Repeat
     *  all", so the screen can greet them with a "let's continue" instead of starting cold. */
    fun requestReplay(
        section: TourSection,
        continuesSequence: Boolean = false,
    ) {
        // A finish reported before this request belongs to an earlier run — an automatic first-visit
        // tour, or a single "Repeat" nobody was waiting on. Left buffered it would make "Repeat all"
        // believe this section is already done and skip straight past it.
        while (replayCompletionChannel.tryReceive().isSuccess) Unit
        synchronized(this) {
            pendingContinuesSequence = continuesSequence
            _pendingReplay.value = section
        }
    }

    /** Clears the pending request only if it is for [section]; another section's request is left
     *  alone. Returns whether the request it cleared was one that continues a "Repeat all"
     *  sequence — `false` when there was nothing of [section]'s to clear. */
    fun consumeReplay(section: TourSection): Boolean =
        synchronized(this) {
            if (_pendingReplay.value != section) return false
            _pendingReplay.value = null
            pendingContinuesSequence.also { pendingContinuesSequence = false }
        }

    fun notifyReplayFinished(section: TourSection) {
        replayCompletionChannel.trySend(section)
    }
}
