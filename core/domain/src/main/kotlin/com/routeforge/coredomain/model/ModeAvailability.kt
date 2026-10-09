package com.routeforge.coredomain.model

/** Whether switching to the inactive playback mode (Guided or Free-roam, whichever isn't
 *  currently active) is currently possible — becomes [UNAVAILABLE_PENDING_RETRY] when that
 *  mode's background recompute fails after an edit (spec 008 FR-011), and [AVAILABLE] again once
 *  a later recompute of it succeeds. View-session state only, never persisted. */
enum class ModeAvailability {
    AVAILABLE,
    UNAVAILABLE_PENDING_RETRY,
}
