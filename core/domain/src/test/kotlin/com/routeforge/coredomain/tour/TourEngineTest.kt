package com.routeforge.coredomain.tour

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun explanatory(isApplicable: Boolean = true) = TourStepStatus(TourStepKind.EXPLANATORY, isApplicable)

private fun action(
    isApplicable: Boolean = true,
    isSatisfied: Boolean = false,
    resultOutcome: TourResultOutcome? = null,
) = TourStepStatus(TourStepKind.ACTION, isApplicable, isSatisfied, resultOutcome)

private fun at(
    index: Int,
    phase: TourPhase,
) = TourPosition(index, phase)

// `settle` loops until a position needs outside input, so what it returns is always a resting
// position (WAITING, PRESENTING, RESULT) or null — never ARRIVING. Rows of the spec's table that
// end in "(index + 1, ARRIVING)" are therefore asserted through the resting position they lead to.
class TourEngineTest {
    @Test
    fun `start is the first step still arriving`() {
        assertEquals(at(0, TourPhase.ARRIVING), TourEngine.Start)
    }

    @Test
    fun `an empty list finishes immediately`() {
        assertNull(TourEngine.settle(emptyList(), TourEngine.Start))
    }

    @Test
    fun `a position past the end finishes`() {
        val steps = listOf(explanatory(), explanatory())

        assertNull(TourEngine.settle(steps, at(2, TourPhase.ARRIVING)))
        assertNull(TourEngine.settle(steps, at(5, TourPhase.PRESENTING)))
    }

    @Test
    fun `a list where nothing is applicable finishes`() {
        val steps = listOf(explanatory(isApplicable = false), action(isApplicable = false), explanatory(isApplicable = false))

        assertNull(TourEngine.settle(steps, TourEngine.Start))
    }

    @Test
    fun `arriving at an inapplicable step moves on to the next one`() {
        val steps = listOf(explanatory(isApplicable = false), action())

        assertEquals(at(1, TourPhase.WAITING), TourEngine.settle(steps, TourEngine.Start))
    }

    @Test
    fun `arriving at an inapplicable last step finishes`() {
        val steps = listOf(explanatory(), explanatory(isApplicable = false))

        assertNull(TourEngine.settle(steps, at(1, TourPhase.ARRIVING)))
    }

    @Test
    fun `arriving at an unsatisfied action waits for the user`() {
        val steps = listOf(action(isSatisfied = false), explanatory())

        assertEquals(at(0, TourPhase.WAITING), TourEngine.settle(steps, TourEngine.Start))
    }

    @Test
    fun `arriving at an explanatory step presents it`() {
        val steps = listOf(explanatory(), explanatory())

        assertEquals(at(0, TourPhase.PRESENTING), TourEngine.settle(steps, TourEngine.Start))
    }

    @Test
    fun `arriving at an already satisfied action skips the action and its result callout`() {
        val steps = listOf(action(isSatisfied = true, resultOutcome = TourResultOutcome.SHOW), explanatory())

        assertEquals(at(1, TourPhase.PRESENTING), TourEngine.settle(steps, TourEngine.Start))
    }

    @Test
    fun `an already satisfied action never reaches waiting or result even as the last step`() {
        val steps = listOf(action(isSatisfied = true, resultOutcome = TourResultOutcome.SHOW))

        assertNull(TourEngine.settle(steps, TourEngine.Start))
    }

    @Test
    fun `waiting on an unsatisfied action stays put`() {
        val steps = listOf(action(isSatisfied = false), explanatory())

        assertEquals(at(0, TourPhase.WAITING), TourEngine.settle(steps, at(0, TourPhase.WAITING)))
    }

    @Test
    fun `waiting on an action that is no longer applicable moves on`() {
        val steps = listOf(action(isApplicable = false), explanatory())

        assertEquals(at(1, TourPhase.PRESENTING), TourEngine.settle(steps, at(0, TourPhase.WAITING)))
    }

    @Test
    fun `waiting on an action that is satisfied and no longer applicable moves on without its callout`() {
        val steps =
            listOf(
                action(isApplicable = false, isSatisfied = true, resultOutcome = TourResultOutcome.SHOW),
                explanatory(),
            )

        assertEquals(at(1, TourPhase.PRESENTING), TourEngine.settle(steps, at(0, TourPhase.WAITING)))
    }

    @Test
    fun `a satisfied action without a result moves on`() {
        val steps = listOf(action(isSatisfied = true, resultOutcome = null), explanatory())

        assertEquals(at(1, TourPhase.PRESENTING), TourEngine.settle(steps, at(0, TourPhase.WAITING)))
    }

    @Test
    fun `a satisfied action without a result as the last step finishes`() {
        val steps = listOf(action(isSatisfied = true, resultOutcome = null))

        assertNull(TourEngine.settle(steps, at(0, TourPhase.WAITING)))
    }

    @Test
    fun `a satisfied action whose result is SHOW presents the result callout`() {
        val steps = listOf(action(isSatisfied = true, resultOutcome = TourResultOutcome.SHOW), explanatory())

        assertEquals(at(0, TourPhase.RESULT), TourEngine.settle(steps, at(0, TourPhase.WAITING)))
    }

    @Test
    fun `a satisfied action whose result is SKIP moves on without a callout`() {
        val steps = listOf(action(isSatisfied = true, resultOutcome = TourResultOutcome.SKIP), explanatory())

        assertEquals(at(1, TourPhase.PRESENTING), TourEngine.settle(steps, at(0, TourPhase.WAITING)))
    }

    @Test
    fun `a satisfied action whose result is SKIP as the last step finishes`() {
        val steps = listOf(action(isSatisfied = true, resultOutcome = TourResultOutcome.SKIP))

        assertNull(TourEngine.settle(steps, at(0, TourPhase.WAITING)))
    }

    @Test
    fun `presenting a step that stops being applicable moves on`() {
        val steps = listOf(explanatory(isApplicable = false), explanatory())

        assertEquals(at(1, TourPhase.PRESENTING), TourEngine.settle(steps, at(0, TourPhase.PRESENTING)))
    }

    @Test
    fun `presenting an applicable step stays put until the user acts`() {
        val steps = listOf(explanatory(), explanatory())

        assertEquals(at(0, TourPhase.PRESENTING), TourEngine.settle(steps, at(0, TourPhase.PRESENTING)))
    }

    @Test
    fun `the result callout stays put until the user acts`() {
        val steps = listOf(action(isSatisfied = true, resultOutcome = TourResultOutcome.SHOW), explanatory())

        assertEquals(at(0, TourPhase.RESULT), TourEngine.settle(steps, at(0, TourPhase.RESULT)))
    }

    @Test
    fun `the result callout stays even if its step stops being applicable`() {
        val steps = listOf(action(isApplicable = false, isSatisfied = true, resultOutcome = TourResultOutcome.SHOW), explanatory())

        assertEquals(at(0, TourPhase.RESULT), TourEngine.settle(steps, at(0, TourPhase.RESULT)))
    }

    @Test
    fun `next from presenting arrives at the following step`() {
        assertEquals(at(3, TourPhase.ARRIVING), TourEngine.onNext(at(2, TourPhase.PRESENTING)))
    }

    @Test
    fun `next from the result callout arrives at the following step`() {
        assertEquals(at(1, TourPhase.ARRIVING), TourEngine.onNext(at(0, TourPhase.RESULT)))
    }

    @Test
    fun `position only ever moves forward across a whole tour`() {
        val firstStep = action(isSatisfied = false, resultOutcome = TourResultOutcome.SHOW)
        val unsatisfied = listOf(firstStep, explanatory(), explanatory())
        val satisfied = listOf(firstStep.copy(isSatisfied = true), explanatory(), explanatory())
        val visited = mutableListOf<TourPosition>()

        var position = TourEngine.settle(unsatisfied, TourEngine.Start)!!.also(visited::add)
        assertEquals(at(0, TourPhase.WAITING), position)

        position = TourEngine.settle(satisfied, position)!!.also(visited::add)
        assertEquals(at(0, TourPhase.RESULT), position)

        position = TourEngine.settle(satisfied, TourEngine.onNext(position))!!.also(visited::add)
        assertEquals(at(1, TourPhase.PRESENTING), position)

        // The action is undone (e.g. the user cancels the mock) — the tour must not go back to it.
        position = TourEngine.settle(unsatisfied, position)!!.also(visited::add)
        assertEquals(at(1, TourPhase.PRESENTING), position)

        position = TourEngine.settle(unsatisfied, TourEngine.onNext(position))!!.also(visited::add)
        assertEquals(at(2, TourPhase.PRESENTING), position)

        assertNull(TourEngine.settle(unsatisfied, TourEngine.onNext(position)))
        assertTrue(visited.zipWithNext().all { (before, after) -> after.index >= before.index })
    }

    @Test
    fun `a step that becomes satisfied while waiting reaches the result only when it has one`() {
        val withResult = listOf(action(isSatisfied = true, resultOutcome = TourResultOutcome.SHOW), explanatory())
        val withoutResult = listOf(action(isSatisfied = true, resultOutcome = null), explanatory())

        assertEquals(at(0, TourPhase.RESULT), TourEngine.settle(withResult, at(0, TourPhase.WAITING)))
        assertEquals(at(1, TourPhase.PRESENTING), TourEngine.settle(withoutResult, at(0, TourPhase.WAITING)))
    }
}
