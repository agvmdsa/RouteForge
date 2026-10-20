package com.routeforge.coredata

import android.content.Context
import com.routeforge.coredomain.MockLocationAuthorizationChecker
import com.routeforge.coredomain.TourProgressRepository
import com.routeforge.coredomain.model.TourSection
import kotlinx.coroutines.flow.MutableStateFlow

private const val PREFS_NAME = "routeforge_tour_progress"
private const val KEY_SEEN_SECTIONS = "seen_sections"

/** The first-run onboarding tour's migration rule (spec 013 FR-010): decides what an install's
 *  "seen sections" should start out as, the very first time this feature's storage key is ever
 *  read. An already-authorized install is treated as existing — it already went through setup
 *  before this feature shipped, so every section starts seen. A not-yet-authorized install is
 *  still working through setup (or is a genuinely fresh install), so every section starts unseen,
 *  letting the automatic first-visit tours trigger normally once setup completes. Pure and
 *  context-free so it's directly unit-testable (Constitution Principle III) without needing
 *  Android's `SharedPreferences`. Reuses [MockLocationAuthorizationChecker] (already a
 *  `core:domain`/`core:data` citizen) rather than `feature:mocklocationsetup`'s own `SetupState`,
 *  since `core:data` must never depend on a feature module. */
internal fun initialSeenSections(isAuthorized: Boolean): Set<TourSection> = if (isAuthorized) TourSection.entries.toSet() else emptySet()

class SharedPreferencesTourProgressRepository(
    context: Context,
    private val mockLocationAuthorizationChecker: MockLocationAuthorizationChecker,
) : TourProgressRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val seenSections = MutableStateFlow(readOrInitializeSeenSections())

    override fun observeSeenSections() = seenSections

    override fun markSeen(section: TourSection) {
        val updated = seenSections.value + section
        persist(updated)
        seenSections.value = updated
    }

    private fun readOrInitializeSeenSections(): Set<TourSection> {
        if (!prefs.contains(KEY_SEEN_SECTIONS)) {
            val initial = initialSeenSections(isAuthorized = mockLocationAuthorizationChecker.isAuthorized())
            persist(initial)
            return initial
        }
        val storedNames = prefs.getStringSet(KEY_SEEN_SECTIONS, emptySet()).orEmpty()
        return storedNames.mapNotNull { name -> runCatching { TourSection.valueOf(name) }.getOrNull() }.toSet()
    }

    private fun persist(sections: Set<TourSection>) {
        prefs.edit().putStringSet(KEY_SEEN_SECTIONS, sections.map { it.name }.toSet()).apply()
    }
}
