package com.routeforge.addresssearch.presentation

import com.routeforge.coredomain.model.NamedPlace
import com.routeforge.coredomain.model.PlaceSearchResult

internal enum class AddressSearchError {
    NETWORK_FAILURE,
}

internal data class AddressSearchState(
    val sourceContext: SearchSourceContext = SearchSourceContext.TELEPORT_OR_FAVORITE,
    val query: String = "",
    val isLoading: Boolean = false,
    /** Distinguishes "haven't searched yet" (plain empty field) from "searched, zero results". */
    val hasSearched: Boolean = false,
    val results: List<PlaceSearchResult> = emptyList(),
    val errorType: AddressSearchError? = null,
    /** Non-null while the Teleport/Save-as-favorite choice is showing for this result
     *  ([sourceContext] == [SearchSourceContext.TELEPORT_OR_FAVORITE] only). */
    val pendingChoiceResult: PlaceSearchResult? = null,
    val isNamingFavorite: Boolean = false,
    val favoriteNameInput: String = "",
    /** The places the user acted on, newest first. */
    val recentPlaces: List<NamedPlace> = emptyList(),
) {
    /** The recent places show while the field is empty or only spaces, and never mixed with results. */
    val showRecents: Boolean get() = query.isBlank() && recentPlaces.isNotEmpty()
}
