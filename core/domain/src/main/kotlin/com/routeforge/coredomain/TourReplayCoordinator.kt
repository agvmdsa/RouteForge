package com.routeforge.coredomain

import com.routeforge.coredomain.model.TourSection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** Coordinates Settings' manual tour replay (spec 013 FR-009) between screens that otherwise don't
 *  know about each other: Settings requests a replay by [TourSection] via [requestReplay], the
 *  matching screen picks it up through [replayRequests] once it's composed and force-starts its own
 *  tour regardless of prior seen-state, then reports back via [notifyReplayFinished] so "Repeat all"
 *  (FR-008) knows when to navigate to the next section. Buffered channels rather than shared state —
 *  a request made just before navigating to the target screen must still be delivered once that
 *  screen's composition starts collecting a moment later. */
class TourReplayCoordinator {
    private val replayRequestChannel = Channel<TourSection>(Channel.BUFFERED)
    val replayRequests: Flow<TourSection> = replayRequestChannel.receiveAsFlow()

    private val replayCompletionChannel = Channel<TourSection>(Channel.BUFFERED)
    val replayCompletions: Flow<TourSection> = replayCompletionChannel.receiveAsFlow()

    fun requestReplay(section: TourSection) {
        replayRequestChannel.trySend(section)
    }

    fun notifyReplayFinished(section: TourSection) {
        replayCompletionChannel.trySend(section)
    }
}
