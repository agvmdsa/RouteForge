package com.routeforge.coredomain.tour

import com.routeforge.coredomain.TourProgressRepository
import com.routeforge.coredomain.TourReplayCoordinator
import com.routeforge.coredomain.model.TourSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the tour of one screen ([section]) is and what happens to it: starts it on a first visit or
 *  a Settings replay, moves it forward as [TourEngine] settles it, and ends it by finishing or
 *  skipping. It knows nothing of drawing, frames or time — the host waits the frames it needs, shows
 *  the greeting for as long as it likes, and tells this class what it saw.
 *
 *  Skip anywhere ends the whole section. Finishing or skipping marks [section] seen and reports it to
 *  [replayCoordinator] so Settings' "Repeat all" can move on — except that a run where no step ever
 *  applied (so the user saw nothing) leaves [section] unseen, to run later once there is something to
 *  point at. */
class TourSession(
    private val section: TourSection,
    private val progressRepository: TourProgressRepository,
    private val replayCoordinator: TourReplayCoordinator,
) {
    private val _position = MutableStateFlow<TourPosition?>(null)
    val position: StateFlow<TourPosition?> = _position.asStateFlow()

    private var hasPresentedAnything = false

    private fun start() {
        hasPresentedAnything = false
        _position.value = TourEngine.Start
    }

    // [markSeen] is false only when nothing at all applied to show: a section the user never got to
    // see must stay unseen so its tour still runs once there is something for it to point at.
    // Settings' "Repeat all" is told either way.
    private fun finish(markSeen: Boolean = true) {
        if (markSeen) progressRepository.markSeen(section)
        replayCoordinator.notifyReplayFinished(section)
        _position.value = null
    }

    fun skip() = finish(markSeen = true)

    fun startIfFirstVisit(seenSections: Set<TourSection>) {
        if (section !in seenSections && _position.value == null) start()
    }

    /** Handles the replay request that is pending ([pendingReplay]): consumes it when it is for this
     *  section and, unless a tour is already running, either starts at once or — when the replay
     *  continues a "Repeat all" sequence and there is a [greeting] — returns it for the host to show, which then
     *  calls [startAfterGreeting]. Returns `null` when there is nothing to show. */
    fun handleReplayRequest(
        pendingReplay: TourSection?,
        greeting: String?,
    ): String? {
        if (pendingReplay != section) return null
        val continuesSequence = replayCoordinator.consumeReplay(section)
        // Already running (the first-visit start got there first): restarting would only
        // flash the first step again.
        if (_position.value != null) return null
        val greetingToShow = greeting.takeIf { continuesSequence }
        if (greetingToShow == null) start()
        return greetingToShow
    }

    fun startAfterGreeting() = start()

    /** Moves the tour to the first position that needs the user, or ends it when [statuses] leave
     *  nothing to show. Called once the host has waited for the screen to settle. */
    fun settle(statuses: List<TourStepStatus>) {
        val current = _position.value ?: return
        val settled = TourEngine.settle(statuses, current)
        if (settled == null) {
            finish(markSeen = hasPresentedAnything)
        } else {
            hasPresentedAnything = true
            if (settled != current) _position.value = settled
        }
    }

    fun next(statuses: List<TourStepStatus>) {
        val current = _position.value ?: return
        val settled = TourEngine.settle(statuses, TourEngine.onNext(current))
        if (settled == null) skip() else _position.value = settled
    }
}
