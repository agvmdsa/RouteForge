package com.routeforge.addresssearch.presentation

import kotlinx.serialization.Serializable

/** Which tab was active when the user opened search — decides what picking a result does
 *  ([com.routeforge.addresssearch.presentation.AddressSearchViewModel]). Lives here, not in
 *  `app`, since [AddressSearchViewModel] must read it without a backward dependency on `app`
 *  (research.md Decision 10 for spec 010). */
@Serializable
enum class SearchSourceContext {
    PLAN_ROUTE,
    OTHER,
}

/** The address-search screen's navigation route — a top-level destination (not nested inside any
 *  tab's own graph), so opening it never disturbs which tab is selected underneath. */
@Serializable
data class AddressSearchRoute(
    val sourceContext: SearchSourceContext,
    val referenceLatitude: Double? = null,
    val referenceLongitude: Double? = null,
)
