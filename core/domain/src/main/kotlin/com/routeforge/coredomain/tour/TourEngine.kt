package com.routeforge.coredomain.tour

enum class TourStepKind { EXPLANATORY, ACTION }

/** Whether an ACTION step's result callout is shown once the action completes. Decided by the
 *  screen from live state: a change that didn't actually happen gets no callout. */
enum class TourResultOutcome { SHOW, SKIP }

/** Everything [TourEngine] needs to know about one step right now — no text, no UI. */
data class TourStepStatus(
    val kind: TourStepKind,
    val isApplicable: Boolean,
    val isSatisfied: Boolean = false,
    val resultOutcome: TourResultOutcome? = null,
)

enum class TourPhase { ARRIVING, WAITING, PRESENTING, RESULT }

/** Where the tour is. [index] points into the full ordered step list, never a filtered view of it. */
data class TourPosition(
    val index: Int,
    val phase: TourPhase,
)

/** Pure decision logic for the onboarding tour: which step to be on and what happens next. Steps
 *  only ever move forward. */
internal object TourEngine {
    val Start = TourPosition(index = 0, phase = TourPhase.ARRIVING)

    /** Advances [position] through everything that needs no user input and returns the first
     *  position that does — [TourPhase.WAITING], [TourPhase.PRESENTING] or [TourPhase.RESULT] — or
     *  `null` once the tour has run past its last step. Never returns [TourPhase.ARRIVING]. */
    fun settle(
        steps: List<TourStepStatus>,
        position: TourPosition,
    ): TourPosition? {
        var current = position
        while (true) {
            val step = steps.getOrNull(current.index) ?: return null
            val next = advance(step, current)
            if (next == current) return current
            current = next
        }
    }

    /** The user pressed Next on an explanatory step or the result callout. */
    fun onNext(position: TourPosition): TourPosition = TourPosition(position.index + 1, TourPhase.ARRIVING)

    private fun advance(
        step: TourStepStatus,
        position: TourPosition,
    ): TourPosition {
        val following = TourPosition(position.index + 1, TourPhase.ARRIVING)
        return when (position.phase) {
            TourPhase.ARRIVING ->
                when {
                    !step.isApplicable -> following
                    step.kind == TourStepKind.EXPLANATORY -> position.copy(phase = TourPhase.PRESENTING)
                    // Already done before the tour got here: skip the action and its callout together.
                    step.isSatisfied -> following
                    else -> position.copy(phase = TourPhase.WAITING)
                }
            TourPhase.WAITING ->
                when {
                    !step.isApplicable -> following
                    !step.isSatisfied -> position
                    step.resultOutcome == TourResultOutcome.SHOW -> position.copy(phase = TourPhase.RESULT)
                    else -> following
                }
            TourPhase.PRESENTING -> if (step.isApplicable) position else following
            TourPhase.RESULT -> position
        }
    }
}
