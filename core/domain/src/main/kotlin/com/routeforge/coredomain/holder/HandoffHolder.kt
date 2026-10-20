package com.routeforge.coredomain.holder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A single-slot hand-off value shared across features without a direct feature-to-feature
 *  dependency — the shared implementation behind every holder in this package. Each holder is a
 *  thin, specifically-named adapter wrapping this rather than a typealias, so call sites and Koin
 *  injection stay unambiguous between different hand-offs that happen to share a value type
 *  (e.g. [SelectedFavoriteWaypointHolder] and [PendingTeleportTargetHolder] are both built on
 *  `HandoffHolder<RoutePoint?>`, but mean different things and must resolve to different
 *  singletons). */
internal class HandoffHolder<T>(
    initial: T,
) {
    private val _value = MutableStateFlow(initial)
    val value: StateFlow<T> = _value.asStateFlow()

    fun set(newValue: T) {
        _value.value = newValue
    }
}
