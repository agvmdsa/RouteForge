package com.routeforge.coredomain.tour

import com.routeforge.coredomain.TourProgressRepository
import com.routeforge.coredomain.TourReplayCoordinator
import com.routeforge.coredomain.model.TourSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class FakeTourProgressRepository(
    initiallySeen: Set<TourSection> = emptySet(),
) : TourProgressRepository {
    private val seenSections = MutableStateFlow(initiallySeen)
    val markedSeen = mutableListOf<TourSection>()

    override fun observeSeenSections(): StateFlow<Set<TourSection>> = seenSections

    override fun markSeen(section: TourSection) {
        markedSeen += section
        seenSections.value = seenSections.value + section
    }
}

private fun explanatory(isApplicable: Boolean = true) = TourStepStatus(TourStepKind.EXPLANATORY, isApplicable)

private fun action(
    isSatisfied: Boolean = false,
    resultOutcome: TourResultOutcome? = null,
) = TourStepStatus(TourStepKind.ACTION, isApplicable = true, isSatisfied = isSatisfied, resultOutcome = resultOutcome)

private fun at(
    index: Int,
    phase: TourPhase,
) = TourPosition(index, phase)

class TourSessionTest {
    private val section = TourSection.SIMULATION
    private val progressRepository = FakeTourProgressRepository()
    private val replayCoordinator = TourReplayCoordinator()
    private val session = TourSession(section, progressRepository, replayCoordinator)

    private fun reportedFinish(): TourSection? = runBlocking { withTimeoutOrNull(100) { replayCoordinator.replayCompletions.first() } }

    @Test
    fun `a section that has not been seen starts on a first visit`() {
        session.startIfFirstVisit(emptySet())

        assertEquals(TourEngine.Start, session.position.value)
    }

    @Test
    fun `a section that was already seen does not start`() {
        session.startIfFirstVisit(setOf(section))

        assertNull(session.position.value)
    }

    @Test
    fun `a tour that is already running is not started again by a first visit`() {
        session.startIfFirstVisit(emptySet())
        session.settle(listOf(explanatory(), explanatory()))

        session.startIfFirstVisit(emptySet())

        assertEquals(at(0, TourPhase.PRESENTING), session.position.value)
    }

    @Test
    fun `settling stops on the first explanatory step that applies and presents it`() {
        session.startIfFirstVisit(emptySet())

        session.settle(listOf(explanatory(), explanatory()))

        assertEquals(at(0, TourPhase.PRESENTING), session.position.value)
    }

    @Test
    fun `settling skips the steps that do not apply`() {
        session.startIfFirstVisit(emptySet())

        session.settle(listOf(explanatory(isApplicable = false), explanatory()))

        assertEquals(at(1, TourPhase.PRESENTING), session.position.value)
    }

    @Test
    fun `next moves to the next step that applies`() {
        val steps = listOf(explanatory(), explanatory(), explanatory())
        session.startIfFirstVisit(emptySet())
        session.settle(steps)

        session.next(steps)

        assertEquals(at(1, TourPhase.PRESENTING), session.position.value)
    }

    @Test
    fun `next after the last step ends the tour, marks the section seen and reports it`() {
        val steps = listOf(explanatory(), explanatory())
        session.startIfFirstVisit(emptySet())
        session.settle(steps)
        session.next(steps)

        session.next(steps)

        assertNull(session.position.value)
        assertEquals(listOf(section), progressRepository.markedSeen)
        assertEquals(section, reportedFinish())
    }

    @Test
    fun `skipping ends the whole section, marks it seen and reports it`() {
        session.startIfFirstVisit(emptySet())
        session.settle(listOf(explanatory(), explanatory()))

        session.skip()

        assertNull(session.position.value)
        assertEquals(listOf(section), progressRepository.markedSeen)
        assertEquals(section, reportedFinish())
    }

    @Test
    fun `a run where no step applied leaves the section unseen but still reports the finish`() {
        session.startIfFirstVisit(emptySet())

        session.settle(listOf(explanatory(isApplicable = false), explanatory(isApplicable = false)))

        assertNull(session.position.value)
        assertTrue(progressRepository.markedSeen.isEmpty())
        assertEquals(section, reportedFinish())
    }

    @Test
    fun `a run that showed a step and then has nothing left to show marks the section seen`() {
        session.startIfFirstVisit(emptySet())
        session.settle(listOf(explanatory()))

        session.settle(listOf(explanatory(isApplicable = false)))

        assertNull(session.position.value)
        assertEquals(listOf(section), progressRepository.markedSeen)
        assertEquals(section, reportedFinish())
    }

    @Test
    fun `an action step that is not done yet keeps the tour waiting on it`() {
        session.startIfFirstVisit(emptySet())

        session.settle(listOf(action(), explanatory()))

        assertEquals(at(0, TourPhase.WAITING), session.position.value)
    }

    @Test
    fun `settling again on the same state leaves the position as it was`() {
        val steps = listOf(action(), explanatory())
        session.startIfFirstVisit(emptySet())
        session.settle(steps)

        session.settle(steps)

        assertEquals(at(0, TourPhase.WAITING), session.position.value)
    }

    @Test
    fun `an action that is done without a callout moves on to the following step`() {
        session.startIfFirstVisit(emptySet())
        session.settle(listOf(action(), explanatory()))

        session.settle(listOf(action(isSatisfied = true, resultOutcome = TourResultOutcome.SKIP), explanatory()))

        assertEquals(at(1, TourPhase.PRESENTING), session.position.value)
    }

    @Test
    fun `an action that is done with a callout shows it and next moves on`() {
        val waiting = listOf(action(), explanatory())
        val done = listOf(action(isSatisfied = true, resultOutcome = TourResultOutcome.SHOW), explanatory())
        session.startIfFirstVisit(emptySet())
        session.settle(waiting)
        session.settle(done)
        assertEquals(at(0, TourPhase.RESULT), session.position.value)

        session.next(done)

        assertEquals(at(1, TourPhase.PRESENTING), session.position.value)
    }

    @Test
    fun `a replay that continues a sequence gives the greeting first and starts after it`() {
        replayCoordinator.requestReplay(section, continuesSequence = true)

        val greeting = session.handleReplayRequest(replayCoordinator.pendingReplay.value, "Let's continue")

        assertEquals("Let's continue", greeting)
        assertNull(session.position.value)
        assertNull(replayCoordinator.pendingReplay.value)

        session.startAfterGreeting()

        assertEquals(TourEngine.Start, session.position.value)
    }

    @Test
    fun `a replay that continues a sequence but has no greeting text starts at once`() {
        replayCoordinator.requestReplay(section, continuesSequence = true)

        val greeting = session.handleReplayRequest(replayCoordinator.pendingReplay.value, null)

        assertNull(greeting)
        assertEquals(TourEngine.Start, session.position.value)
    }

    @Test
    fun `a plain replay starts at once without a greeting`() {
        replayCoordinator.requestReplay(section)

        val greeting = session.handleReplayRequest(replayCoordinator.pendingReplay.value, "Let's continue")

        assertNull(greeting)
        assertEquals(TourEngine.Start, session.position.value)
        assertNull(replayCoordinator.pendingReplay.value)
    }

    @Test
    fun `a replay request while the tour is already running is consumed and does not restart it`() {
        session.startIfFirstVisit(emptySet())
        session.settle(listOf(explanatory(), explanatory()))
        replayCoordinator.requestReplay(section, continuesSequence = true)

        val greeting = session.handleReplayRequest(replayCoordinator.pendingReplay.value, "Let's continue")

        assertNull(greeting)
        assertEquals(at(0, TourPhase.PRESENTING), session.position.value)
        assertNull(replayCoordinator.pendingReplay.value)
    }

    @Test
    fun `a replay request for another section is left alone`() {
        replayCoordinator.requestReplay(TourSection.ROUTE_CREATION)

        val greeting = session.handleReplayRequest(replayCoordinator.pendingReplay.value, "Let's continue")

        assertNull(greeting)
        assertNull(session.position.value)
        assertEquals(TourSection.ROUTE_CREATION, replayCoordinator.pendingReplay.value)
    }
}
