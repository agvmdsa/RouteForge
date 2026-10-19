package com.routeforge.addresssearch.presentation

import kotlinx.serialization.Serializable

/** Which tab was active when the user opened search — decides what picking a result does
 *  ([com.routeforge.addresssearch.presentation.AddressSearchViewModel]). Lives here, not in
 *  `app`, since [AddressSearchViewModel] must read it without a backward dependency on `app`
 *  (research.md Decision 10 for spec 010). */
@Serializable
enum class SearchSourceContext {
    PLAN_ROUTE,
    TELEPORT_OR_FAVORITE,
}

/** The address-search screen's navigation route — a top-level destination (not nested inside any
 *  tab's own graph), so opening it never disturbs which tab is selected underneath. */
@Serializable
data class AddressSearchRoute(
    val sourceContext: SearchSourceContext,
    val referenceLatitude: Double? = null,
    val referenceLongitude: Double? = null,
    /** A place that arrived from another app (a link, a share): the screen opens straight on the choice for it. Kept as three
     *  plain values because a typed route cannot carry a class of its own without extra navigation plumbing. */
    val initialLatitude: Double? = null,
    val initialLongitude: Double? = null,
    val initialName: String? = null,
    /** Text that arrived from another app and led to no place: the screen opens with it typed in and searched. */
    val initialQuery: String? = null,
)

/** A place that arrived from outside the app, with its name when it has one. */
data class InitialPlace(
    val latitude: Double,
    val longitude: Double,
    val name: String?,
)

fun AddressSearchRoute.initialPlace(): InitialPlace? =
    if (initialLatitude != null && initialLongitude != null) InitialPlace(initialLatitude, initialLongitude, initialName) else null
