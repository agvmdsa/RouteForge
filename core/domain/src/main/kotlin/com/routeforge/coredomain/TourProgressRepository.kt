package com.routeforge.coredomain

import com.routeforge.coredomain.model.TourSection
import kotlinx.coroutines.flow.StateFlow

/** Tracks which first-run onboarding tour sections (spec 013-first-run-onboarding-tour) the user
 *  has already seen — by completing, skipping, or manually replaying them. There is no "mark
 *  unseen" operation: a manual replay from Settings runs identically to the original automatic run
 *  regardless of prior seen-state, so nothing needs to be reset before it. */
interface TourProgressRepository {
    fun observeSeenSections(): StateFlow<Set<TourSection>>

    fun markSeen(section: TourSection)
}
